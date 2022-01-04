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
        String responseBody = null;
        try {
            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, prepareHeader(username, password), String.class);
            responseBody = responseEntity.getBody();
        } catch (Exception e) {
            //log message is publishing as an info log, this is just to make it visible.
            log.error(e.getMessage());
        }
        return responseBody;
    }

    public String put(String url, String username, String password){

        return "";
    }

    private HttpEntity<String> prepareHeader(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setBasicAuth(username, password);
        headers.add("traceId", tracer.getTraceId());

        return new HttpEntity<>(headers);
    }
}
