package ee.cyber.xroad.mediator.service.wsdlmerge.merger;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ee.cyber.sdsb.common.CodedException;
import ee.cyber.sdsb.common.identifier.ClientId;
import ee.cyber.xroad.mediator.IdentifierMapping;

import static ee.cyber.sdsb.common.ErrorCodes.X_ADAPTER_WSDL_NOT_FOUND;
/**
 * Responsible of end-to-end functionality of WSDLs merging - takes list of WSDL
 * URL-s as input and returns input stream of merged WSDL-s as output.
 */
public class WSDLsMerger {

    private static final Logger LOG = LoggerFactory
            .getLogger(WSDLsMerger.class);

    private List<String> wsdlUrls;
    private WSDLProvider wsdlProvider;
    private ClientId client;

    @Getter
    private InputStream mergedWsdlAsStream;

    public WSDLsMerger(
            List<String> wsdlUrls,
            WSDLProvider wsdlProvider,
            ClientId client) throws Exception {
        this.wsdlUrls = wsdlUrls;
        this.wsdlProvider = wsdlProvider;
        this.client = client;

        mergeWsdls();
    }

    private void mergeWsdls() throws Exception {
        if (wsdlUrls == null || wsdlUrls.isEmpty()) {
            throw new CodedException(X_ADAPTER_WSDL_NOT_FOUND,
                    "No adapter WSDL-s found for client '%s', inspect Your "
                            + "server configuration.", client);
        }

        if (wsdlUrls.size() == 1) {
            sendWsdlBackUnchanged();
            return;
        }

        mergeMultipleWsdls();
    }

    private void sendWsdlBackUnchanged() throws IOException {
        LOG.trace("sendWsdlBackUnchanged()");
        mergedWsdlAsStream = wsdlProvider.getWsdl(wsdlUrls.get(0));
    }

    private void mergeMultipleWsdls() throws Exception {
        LOG.trace("mergeMultipleWsdls()");
        List<InputStream> wsdlInputStreams = new ArrayList<>(wsdlUrls.size());

        for (String each : wsdlUrls) {
            wsdlInputStreams.add(wsdlProvider.getWsdl(each));
        }

        String v5DbName = IdentifierMapping.getInstance().getShortName(client);
        mergedWsdlAsStream =
                new WSDLStreamsMerger(wsdlInputStreams, v5DbName)
                        .getMergedWsdlAsStream();
    }
}