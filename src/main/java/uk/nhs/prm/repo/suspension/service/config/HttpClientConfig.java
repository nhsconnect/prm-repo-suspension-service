package uk.nhs.prm.repo.suspension.service.config;

import org.apache.hc.client5.http.ConnectionKeepAliveStrategy;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustSelfSignedStrategy;
import org.apache.hc.core5.http.HeaderElement;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.message.BasicHeaderElementIterator;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.apache.http.protocol.HTTP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

@Configuration
public class HttpClientConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpClientConfig.class);

    //@formatter:off
    private static final int CONNECT_TIMEOUT = Integer.getInteger("HTTP_CONNECT_TIMEOUT", 45_000);
    private static final int REQUEST_TIMEOUT = Integer.getInteger("HTTP_REQUEST_TIMEOUT", 45_000);
    private static final int SOCKET_TIMEOUT = Integer.getInteger("HTTP_REQUEST_TIMEOUT", REQUEST_TIMEOUT);
    private static final int MAX_TOTAL_CONNECTIONS = Integer.getInteger("MAX_TOTAL_CONNECTIONS", 50);
    private static final int DEFAULT_KEEP_ALIVE_TIME_MILLIS
            = Integer.getInteger("DEFAULT_KEEP_ALIVE_TIME_MILLIS", 20_000);
    private static final int CLOSE_IDLE_CONNECTION_WAIT_TIME_SECS
            = Integer.getInteger("DEFAULT_KEEP_ALIVE_TIME_MILLIS", DEFAULT_KEEP_ALIVE_TIME_MILLIS);
    //@formatter:on

    @Bean
    public PoolingHttpClientConnectionManager poolingConnectionManager() {
        SSLContextBuilder builder = new SSLContextBuilder();
        try {
            builder.loadTrustMaterial(null, new TrustSelfSignedStrategy());
        }
        catch (NoSuchAlgorithmException | KeyStoreException e) {
            LOGGER.error("Pooling Connection Manager Initialisation failure because of "
                    + e.getMessage(), e);
        }

        SSLConnectionSocketFactory sslsf = null;
        try {
            sslsf = new SSLConnectionSocketFactory(builder.build());
        }
        catch (KeyManagementException | NoSuchAlgorithmException e) {
            LOGGER.error("Pooling Connection Manager Initialisation failure because of "
                    + e.getMessage(), e);
        }

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
                .<ConnectionSocketFactory> create().register("https", sslsf)
                .register("http", new PlainConnectionSocketFactory())
                .build();

        PoolingHttpClientConnectionManager poolingConnectionManager =
                new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        poolingConnectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
        return poolingConnectionManager;
    }

    @Bean
    public ConnectionKeepAliveStrategy connectionKeepAliveStrategy() {
        return (response, context) -> {
            BasicHeaderElementIterator it = new BasicHeaderElementIterator(response.headerIterator(HTTP.CONN_KEEP_ALIVE));

            while (it.hasNext()) {
                HeaderElement headerElement = it.next();
                String param = headerElement.getName();
                String value = headerElement.getValue();

                if (value != null && param.equalsIgnoreCase("timeout")) {
                    return TimeValue.ofMilliseconds(Long.parseLong(value) * 1000);
                }
            }
            return TimeValue.ofMilliseconds(DEFAULT_KEEP_ALIVE_TIME_MILLIS);
        };
    }

    @Bean
    public CloseableHttpClient httpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofMilliseconds(REQUEST_TIMEOUT))
                .setConnectTimeout(Timeout.ofMilliseconds(CONNECT_TIMEOUT))
                .setResponseTimeout(Timeout.ofMilliseconds(SOCKET_TIMEOUT))
                .build();

        return HttpClients.custom().setDefaultRequestConfig(requestConfig)
                .setConnectionManager(poolingConnectionManager())
                .setKeepAliveStrategy(connectionKeepAliveStrategy())
                .build();
    }

    @Bean
    public Runnable idleConnectionMonitor(final PoolingHttpClientConnectionManager connectionManager) {
        return new Runnable() {

            @Override
            @Scheduled(fixedDelay = 10000)
            public void run() {
                try {
                    if (connectionManager != null) {
                        LOGGER.trace(
                                "run IdleConnectionMonitor - Closing expired and idle connections...");
                        connectionManager.closeExpired();
                        connectionManager.closeIdle(TimeValue.ofSeconds(CLOSE_IDLE_CONNECTION_WAIT_TIME_SECS));
                    }
                    else {
                        LOGGER.trace(
                                "run IdleConnectionMonitor - Http Client Connection manager is not initialised");
                    }
                }
                catch (Exception e) {
                    LOGGER.error("run IdleConnectionMonitor - Exception occurred. msg={}, e={}",
                            e.getMessage(), e);
                }
            }
        };
    }
}
