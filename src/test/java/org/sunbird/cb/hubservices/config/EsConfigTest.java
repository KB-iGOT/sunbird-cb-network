package org.sunbird.cb.hubservices.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.sunbird.cb.hubservices.util.ConnectionProperties;

class EsConfigTest {

    @InjectMocks
    private EsConfig esConfig;

    @Mock
    private ConnectionProperties connectionProperties;

    @Mock
    private RestClientBuilder restClientBuilder;

    @Mock
    private RestHighLevelClient restHighLevelClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

//    @Test
//    void testRestHighLevelClient() {
//        when(connectionProperties.getEsHost()).thenReturn("localhost");
//        when(connectionProperties.getEsPort()).thenReturn("9200");
//        when(connectionProperties.getEsUser()).thenReturn("elastic");
//        when(connectionProperties.getEsPassword()).thenReturn("password");
//
//        try (MockedStatic<RestClient> restClientMock = mockStatic(RestClient.class);
//             MockedStatic<BasicCredentialsProvider> credProviderMock = mockStatic(BasicCredentialsProvider.class)) {
//
//            BasicCredentialsProvider mockCredentialsProvider = mock(BasicCredentialsProvider.class);
//            credProviderMock.when(BasicCredentialsProvider::new).thenReturn(mockCredentialsProvider);
//
//            HttpHost expectedHost = new HttpHost("localhost", 9200);
//            restClientMock.when(() -> RestClient.builder(any(HttpHost.class))).thenReturn(restClientBuilder);
//            when(restClientBuilder.setHttpClientConfigCallback(any())).thenReturn(restClientBuilder);
//
//            try (MockedStatic<RestHighLevelClient> clientMock = mockStatic(RestHighLevelClient.class)) {
//                clientMock.when(() -> new RestHighLevelClient(restClientBuilder)).thenReturn(restHighLevelClient);
//
//                RestHighLevelClient result = esConfig.restHighLevelClient(connectionProperties);
//
//                assertEquals(restHighLevelClient, result);
//                verify(mockCredentialsProvider).setCredentials(eq(AuthScope.ANY), any(UsernamePasswordCredentials.class));
//                verify(restClientBuilder).setHttpClientConfigCallback(any());
//            }
//        }
//    }
//
//    @Test
//    void testRestHighLevelClient_DifferentPort() {
//        when(connectionProperties.getEsHost()).thenReturn("elasticsearch.example.com");
//        when(connectionProperties.getEsPort()).thenReturn("9300");
//        when(connectionProperties.getEsUser()).thenReturn("admin");
//        when(connectionProperties.getEsPassword()).thenReturn("secret");
//
//        try (MockedStatic<RestClient> restClientMock = mockStatic(RestClient.class);
//             MockedStatic<BasicCredentialsProvider> credProviderMock = mockStatic(BasicCredentialsProvider.class)) {
//
//            BasicCredentialsProvider mockCredentialsProvider = mock(BasicCredentialsProvider.class);
//            credProviderMock.when(BasicCredentialsProvider::new).thenReturn(mockCredentialsProvider);
//
//            restClientMock.when(() -> RestClient.builder(any(HttpHost.class))).thenReturn(restClientBuilder);
//            when(restClientBuilder.setHttpClientConfigCallback(any())).thenReturn(restClientBuilder);
//
//            try (MockedStatic<RestHighLevelClient> clientMock = mockStatic(RestHighLevelClient.class)) {
//                clientMock.when(() -> new RestHighLevelClient(restClientBuilder)).thenReturn(restHighLevelClient);
//
//                RestHighLevelClient result = esConfig.restHighLevelClient(connectionProperties);
//
//                assertEquals(restHighLevelClient, result);
//                verify(mockCredentialsProvider).setCredentials(eq(AuthScope.ANY), any(UsernamePasswordCredentials.class));
//            }
//        }
//    }

    @Test
    void testRestHighLevelClient_InvalidPort() {
        when(connectionProperties.getEsHost()).thenReturn("localhost");
        when(connectionProperties.getEsPort()).thenReturn("invalid");
        when(connectionProperties.getEsUser()).thenReturn("elastic");
        when(connectionProperties.getEsPassword()).thenReturn("password");

        assertThrows(NumberFormatException.class, () -> {
            esConfig.restHighLevelClient(connectionProperties);
        });
    }
}