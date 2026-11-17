package utils;

import java.io.ByteArrayInputStream;
import java.net.CookieHandler;
import java.net.Socket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

import config.Config;

public class HttpClientCustom {

    private HttpClientCustom() {

    }

    public static HttpClient basicClient() {

        return HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public static HttpClient buildClientForDashboard(CookieHandler cookieHandler) {

        SSLContext sslContext = buildCompositeSslContext();
        var builder = HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(10));

        if (sslContext != null) {
            builder.sslContext(sslContext);
        }
        if (cookieHandler != null) {
            builder.cookieHandler(cookieHandler);
        }

        return builder.build();
    }

    @SuppressWarnings({"java:S3776","complex method for easy access"})
    public static SSLContext buildCompositeSslContext() {

        try {
            // 1. Parse hardcoded certificate
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream certStream = new ByteArrayInputStream(
                    Config.HARDCODED_CERT_PEM.getBytes(StandardCharsets.UTF_8)
            );
            X509Certificate hardcodedCert = (X509Certificate) cf.generateCertificate(certStream);

            // 2. Default JVM TrustManager
            TrustManagerFactory defaultTmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm()
            );
            defaultTmf.init((KeyStore) null);
            X509TrustManager defaultTm = null;
            for (TrustManager tm : defaultTmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager x509Tm) {
                    defaultTm = x509Tm;
                    break;
                }
            }

            final X509TrustManager finalDefaultTm = defaultTm;

            X509ExtendedTrustManager compositeTm = new X509ExtendedTrustManager() {

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
                    // no ops
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
                    checkTrusted(chain, authType);
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
                    // no ops
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
                    checkTrusted(chain, authType);
                }

                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                    // no ops
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    checkTrusted(chain, authType);
                }

                private void checkTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                    if (finalDefaultTm != null) {
                        try {
                            finalDefaultTm.checkServerTrusted(chain, authType);
                            return;
                        } catch (CertificateException ignored) {
                            // no ops
                        }
                    }

                    if (chain != null && chain.length > 0) {
                        if (chain[0].equals(hardcodedCert)) {
                            return;
                        }
                        String subject = chain[0].getSubjectX500Principal().getName();
                        String issuer = chain[0].getIssuerX500Principal().getName();
                        if (subject.contains("sfr.com") || issuer.contains("SFR")) {
                            return;
                        }
                    }

                    throw new CertificateException("Certificate not trusted");
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return finalDefaultTm != null ? finalDefaultTm.getAcceptedIssuers() : new X509Certificate[0];
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{ compositeTm }, new SecureRandom());
            return sslContext;

        } catch (Exception e) {
            PluginLogger.error("Failed to initialize composite SSL context, falling back to default", e);
            try {
                return SSLContext.getDefault();
            } catch (Exception ex) {
                return null;
            }
        }
    }

    public static HttpClient buildClientWithCert() {

        try {
            // 1. Parse the hardcoded certificate string into an X509Certificate
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            ByteArrayInputStream certStream = new ByteArrayInputStream(
                    Config.HARDCODED_CERT_PEM.getBytes(StandardCharsets.UTF_8)
            );
            X509Certificate cert = (X509Certificate) cf.generateCertificate(certStream);

            // 2. Create an in-memory KeyStore and put the certificate in it
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null); // initialize empty in-memory keystore
            keyStore.setCertificateEntry("hardcoded-cert", cert);

            // 3. Initialize TrustManagerFactory with our in-memory KeyStore
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm()
            );
            tmf.init(keyStore);

            // 4. Create an SSLContext that trusts only our keystore
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            return HttpClient.newBuilder()
                             .sslContext(sslContext)
                             .connectTimeout(Duration.ofSeconds(5))
                             .build();
        } catch (Exception e) {
            PluginLogger.error("Failed to initialize SSL context with hardcoded cert, falling back to default", e);
            return HttpClient.newBuilder()
                             .connectTimeout(Duration.ofSeconds(5))
                             .build();
        }
    }

    @SuppressWarnings({ "java:S4830", "Unsafe client, for testing purpose" })
    public static HttpClient buildInsecureClient() {

        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509ExtendedTrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
                            // no ops
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
                            // no ops
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
                            // no ops
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
                            // no ops
                        }

                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) {
                            // no ops
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) {
                            // no ops
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {

                            return new X509Certificate[0];
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            return HttpClient.newBuilder()
                             .sslContext(sslContext)
                             .connectTimeout(Duration.ofSeconds(5))
                             .build();
        } catch (GeneralSecurityException e) {
            PluginLogger.error("Failed to initialize insecure SSL context, falling back to default", e);
            return HttpClient.newBuilder()
                             .connectTimeout(Duration.ofSeconds(5))
                             .build();
        }
    }
}
