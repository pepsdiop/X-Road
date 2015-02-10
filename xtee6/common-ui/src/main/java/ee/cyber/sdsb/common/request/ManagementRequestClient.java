package ee.cyber.sdsb.common.request;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedKeyManager;
import javax.net.ssl.X509TrustManager;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import ee.cyber.sdsb.common.SystemProperties;
import ee.cyber.sdsb.common.conf.InternalSSLKey;
import ee.cyber.sdsb.common.conf.globalconf.GlobalConf;
import ee.cyber.sdsb.common.util.CryptoUtils;
import ee.cyber.sdsb.common.util.HttpSender;
import ee.cyber.sdsb.common.util.StartStop;

/**
 * Client that sends managements requests to the Central Server.
 */
@Slf4j
public final class ManagementRequestClient implements StartStop {

    // HttpClient configuration parameters.
    private static final int CLIENT_MAX_TOTAL_CONNECTIONS = 100;
    private static final int CLIENT_MAX_CONNECTIONS_PER_ROUTE = 25;

    private CloseableHttpClient centralHttpClient;
    private CloseableHttpClient proxyHttpClient;

    private static ManagementRequestClient instance =
            new ManagementRequestClient();

    /**
     * @return the singleton ManagementRequestClient
     */
    public static ManagementRequestClient getInstance() {
        return instance;
    }

    static HttpSender createCentralHttpSender() {
        return new HttpSender(getInstance().centralHttpClient);
    }

    static HttpSender createProxyHttpSender() {
        return new HttpSender(getInstance().proxyHttpClient);
    }

    private ManagementRequestClient() {
        try {
            createCentralHttpClient();
            createProxyHttpClient();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Unable to initialize management request client", e);
        }
    }

    @Override
    public void start() throws Exception {
        log.info("Starting ManagementRequestClient...");
    }

    @Override
    public void stop() throws Exception {
        log.info("Stopping ManagementRequestClient...");

        IOUtils.closeQuietly(proxyHttpClient);
        IOUtils.closeQuietly(centralHttpClient);
    }

    @Override
    public void join() throws InterruptedException {
    }

    // -- Helper methods ------------------------------------------------------

    private void createCentralHttpClient() throws Exception {
        log.trace("createCentralHttpClient()");

        TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain,
                    String authType) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] chain,
                    String authType) throws CertificateException {
                if (chain.length == 0) {
                    throw new CertificateException(
                            "Central server did not send SSL certificate");
                }

                X509Certificate centralServerSslCert = null;
                try {
                    centralServerSslCert =
                            GlobalConf.getCentralServerSslCertificate();
                } catch (Exception e) {
                    throw new CertificateException("Could not get central "
                            + "server SSL certificate from global conf", e);
                }

                if (centralServerSslCert == null) {
                    throw new CertificateException(
                            "Central server SSL certificate "
                                    + "is not in global conf");
                }

                if (!centralServerSslCert.equals(chain[0])) {
                    throw new CertificateException(
                            "Central server SSL certificate "
                                    + "does not match in global conf");
                }
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        centralHttpClient = createHttpClient(null, tm);
    }

    private void createProxyHttpClient() throws Exception {
        log.trace("createProxyHttpClient()");

        TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] chain,
                    String authType) throws CertificateException {
            }
            @Override
            public void checkServerTrusted(X509Certificate[] chain,
                    String authType) throws CertificateException {

            }
            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        KeyManager km = new X509ExtendedKeyManager() {

            private static final String ALIAS = "MgmtAuthKeyManager";

            @Override
            public String chooseClientAlias(String[] keyType,
                    Principal[] issuers, Socket socket) {
                return ALIAS;
            }

            @Override
            public String chooseServerAlias(String keyType,
                    Principal[] issuers, Socket socket) {
                return ALIAS;
            }

            @Override
            public X509Certificate[] getCertificateChain(String alias) {
                try {
                    return new X509Certificate[] {
                            InternalSSLKey.load().getCert() };
                } catch (Exception e) {
                    log.error("Failed to load internal SSL key", e);
                    return new X509Certificate[] {};
                }
            }

            @Override
            public String[] getClientAliases(String keyType,
                    Principal[] issuers) {
                return null;
            }

            @Override
            public PrivateKey getPrivateKey(String alias) {
                try {
                    return InternalSSLKey.load().getKey();
                } catch (Exception e) {
                    log.error("Failed to load internal SSL key", e);
                    return null;
                }
            }

            @Override
            public String[] getServerAliases(String keyType,
                    Principal[] issuers) {
                return null;
            }

            @Override
            public String chooseEngineClientAlias(String[] keyType,
                    Principal[] issuers, SSLEngine engine) {
                return ALIAS;
            }

            @Override
            public String chooseEngineServerAlias(String keyType,
                    Principal[] issuers, SSLEngine engine) {
                return ALIAS;
            }
        };

        proxyHttpClient = createHttpClient(km, tm);
    }

    private static CloseableHttpClient createHttpClient(KeyManager km,
            TrustManager tm) throws Exception {
        RegistryBuilder<ConnectionSocketFactory> sfr =
                RegistryBuilder.<ConnectionSocketFactory>create();

        sfr.register("http", PlainConnectionSocketFactory.INSTANCE);

        SSLContext ctx = SSLContext.getInstance(CryptoUtils.SSL_PROTOCOL);
        ctx.init(km != null  ? new KeyManager[] {km} : null,
                tm != null  ? new TrustManager[] {tm} : null,
                        new SecureRandom());

        SSLConnectionSocketFactory sf =
                new SSLConnectionSocketFactory(ctx,
                        SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

        sfr.register("https", sf);

        PoolingHttpClientConnectionManager cm =
                new PoolingHttpClientConnectionManager(sfr.build());
        cm.setMaxTotal(CLIENT_MAX_TOTAL_CONNECTIONS);
        cm.setDefaultMaxPerRoute(CLIENT_MAX_CONNECTIONS_PER_ROUTE);

        int timeout = SystemProperties.getClientProxyTimeout();
        RequestConfig.Builder rb = RequestConfig.custom();
        rb.setConnectTimeout(timeout);
        rb.setConnectionRequestTimeout(timeout);
        rb.setStaleConnectionCheckEnabled(false);

        HttpClientBuilder cb = HttpClients.custom();
        cb.setConnectionManager(cm);
        cb.setDefaultRequestConfig(rb.build());

        // Disable request retry
        cb.setRetryHandler(new DefaultHttpRequestRetryHandler(0, false));

        return cb.build();
    }
}