package org.sunbird.cb.hubservices;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.cassandra.CassandraDataAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;
import org.sunbird.cb.hubservices.util.ConnectionProperties;

@SpringBootApplication(exclude = CassandraDataAutoConfiguration.class)
@EnableAsync
public class HubServiceApplication {


    private final ConnectionProperties connectionProperties;

    public HubServiceApplication(ConnectionProperties serverProperties) {
        this.connectionProperties = serverProperties;
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate(getClientHttpRequestFactory());
    }


    private ClientHttpRequestFactory getClientHttpRequestFactory() {
        int timeout = connectionProperties.getClientHttpRequestFactoryTimeout();
        RequestConfig config = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMilliseconds(timeout))
                .build();
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(connectionProperties.getMaxTotalConnections());
        cm.setDefaultMaxPerRoute(connectionProperties.getMaxConnectionsPerRoute());
        CloseableHttpClient client = HttpClients.custom()
                .setDefaultRequestConfig(config)
                .setConnectionManager(cm)
                .build();
        return new HttpComponentsClientHttpRequestFactory(client);
    }

	public static void main(String[] args) {
		SpringApplication.run(HubServiceApplication.class, args);

	}

}
