package utils;

import java.io.ByteArrayInputStream;
import java.net.Socket;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509ExtendedTrustManager;

import com.intellij.openapi.diagnostic.Logger;

import config.Config;

public class HttpClientCustom {

    private static final Logger LOG = Logger.getInstance(HttpClientCustom.class);

    private HttpClientCustom() {

    }

    public static HttpClient basicClient() {

        return HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
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
            LOG.error("Failed to initialize SSL context with hardcoded cert, falling back to default", e);
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
            LOG.error("Failed to initialize insecure SSL context, falling back to default", e);
            return HttpClient.newBuilder()
                             .connectTimeout(Duration.ofSeconds(5))
                             .build();
        }
    }
}
