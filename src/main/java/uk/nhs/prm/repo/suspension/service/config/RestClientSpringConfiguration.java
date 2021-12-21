package uk.nhs.prm.repo.suspension.service.config;

import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class RestClientSpringConfiguration {
    @Autowired
    CloseableHttpClient httpClient;

    @Value("${pdsAdaptor.serviceUrl}")
    private String apiHost;

    @Value("${pdsAdaptor.suspensionService.password}")
    private String suspensionServicePassword;

    private static final String SUSPENSION_SERVICE_USERNAME = "suspension-service";

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate(clientHttpRequestFactory());

        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(apiHost));
        return restTemplate;
    }

    @Bean
    RestOperations restTemplateBuilder(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.basicAuthentication(SUSPENSION_SERVICE_USERNAME, suspensionServicePassword).build();
    }

    @Bean
    @ConditionalOnMissingBean
    public HttpComponentsClientHttpRequestFactory clientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory
                = new HttpComponentsClientHttpRequestFactory();
        clientHttpRequestFactory.setHttpClient(httpClient);
        return clientHttpRequestFactory;
    }
}
