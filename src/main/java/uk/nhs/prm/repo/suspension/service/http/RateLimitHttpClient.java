package uk.nhs.prm.repo.suspension.service.http;

import com.google.common.util.concurrent.RateLimiter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class RateLimitHttpClient {

    private final HttpServiceClient httpServiceClient;

    private final RateLimiter rateLimiter = RateLimiter.create(2.0);

    public ResponseEntity<String> getWithStatusCodeNoRateLimit(String url, String username, String password) {
        return httpServiceClient.getWithStatusCode(url, username, password);
    }

    public ResponseEntity<String> putWithStatusCodeWithTwoSecRateLimit(String url, String username, String password, Object requestPayload) {
        rateLimiter.acquire();
        return httpServiceClient.putWithStatusCode(url, username, password, requestPayload);
    }
}
