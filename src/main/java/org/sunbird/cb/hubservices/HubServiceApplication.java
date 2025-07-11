package org.sunbird.cb.hubservices;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
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
        RequestConfig config = RequestConfig.custom().setConnectTimeout(timeout).setConnectionRequestTimeout(timeout)
                .setSocketTimeout(timeout).build();
        CloseableHttpClient client = HttpClientBuilder.create().setMaxConnTotal(connectionProperties.getClientHttpRequestFactoryPoolingMaxTotalConnections()).setMaxConnPerRoute(connectionProperties.getClientHttpRequestFactoryPoolingMaxTotalConnections())
                .setDefaultRequestConfig(config).build();
        HttpComponentsClientHttpRequestFactory cRequestFactory = new HttpComponentsClientHttpRequestFactory(client);
        cRequestFactory.setReadTimeout(timeout);
        return cRequestFactory;
    }

	public static void main(String[] args) {
		SpringApplication.run(HubServiceApplication.class, args);

	}

}
