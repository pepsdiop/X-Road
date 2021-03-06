/**
 * The MIT License
 * Copyright (c) 2015 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ee.ria.xroad.common.certificateprofile.impl;

import java.security.cert.X509Certificate;

import ee.ria.xroad.common.certificateprofile.AuthCertificateProfileInfo;
import ee.ria.xroad.common.certificateprofile.CertificateProfileInfoProvider;
import ee.ria.xroad.common.certificateprofile.SignCertificateProfileInfo;
import ee.ria.xroad.common.identifier.ClientId;
import ee.ria.xroad.common.util.CertUtils;

/**
 * Default implementation of CertificateProfileInfoProvider.
 */
public class TestCertificateProfileInfoProvider
        implements CertificateProfileInfoProvider {

    @Override
    public AuthCertificateProfileInfo getAuthCertProfile(
            AuthCertificateProfileInfo.Parameters params) {
        return new TestAuthCertificateProfileInfo(params);
    }

    @Override
    public SignCertificateProfileInfo getSignCertProfile(
            SignCertificateProfileInfo.Parameters params) {
        return new TestSignCertificateProfileInfo(params);
    }

    static class TestAuthCertificateProfileInfo
            extends EjbcaAuthCertificateProfileInfo {

        TestAuthCertificateProfileInfo(Parameters params) {
            super(params);
        }
    }

    static class TestSignCertificateProfileInfo
            extends EjbcaSignCertificateProfileInfo {

        TestSignCertificateProfileInfo(Parameters params) {
            super(params);
        }

        @Override
        public ClientId getSubjectIdentifier(X509Certificate certificate) {
            return ClientId.create(
                params.getClientId().getXRoadInstance(),
                "BUSINESS",
                CertUtils.getSubjectCommonName(certificate)
            );
        }
    }
}
