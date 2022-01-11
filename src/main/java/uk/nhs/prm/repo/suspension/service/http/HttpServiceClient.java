package uk.nhs.prm.repo.suspension.service.http;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import uk.nhs.prm.repo.suspension.service.config.Tracer;

import java.util.Arrays;

@Component
@AllArgsConstructor
@Slf4j
public class HttpServiceClient {

    private RestTemplate restTemplate;
    private final Tracer tracer;

    public String get(String url, String username, String password) {
        HttpEntity<String> noPayloadEntity = new HttpEntity<>(createSharedHeaders(username, password));
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, noPayloadEntity, String.class);
        return responseEntity.getBody();
    }

    public String put(String url, String username, String password, Object requestPayload) {
        var requestEntity = new HttpEntity<>(requestPayload, createSharedHeaders(username, password));
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, String.class);
        return responseEntity.getBody();
    }

    private HttpHeaders createSharedHeaders(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setBasicAuth(username, password);
        headers.add("traceId", tracer.getTraceId());
        return headers;
    }
}
