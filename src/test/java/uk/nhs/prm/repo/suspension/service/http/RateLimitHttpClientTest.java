package uk.nhs.prm.repo.suspension.service.http;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith({MockitoExtension.class})
class RateLimitHttpClientTest {

    @Mock
    private HttpServiceClient httpServiceClient;

    @InjectMocks
    private RateLimitHttpClient rateLimitClient;

    @Test
    void shouldCallServiceClientGetWithNoRateLimit() {
        int numberOfThreads = 10;
        ArrayList<Thread> threadsWithPutRequest = createThreadsWithGetRequest(numberOfThreads);

        Instant startTime = Instant.now();
        threadsWithPutRequest.forEach(Thread::start);

        await().untilAsserted(() ->
                verify(httpServiceClient, times(numberOfThreads)).getWithStatusCode("url",
                        "username", "password"));

        Instant finishTime = Instant.now();

        Duration processingTime = Duration.between(startTime, finishTime);

        assertThat(processingTime).isLessThan(Duration.ofMillis(500));
    }

    @Test
    @Disabled
    void shouldCallServiceClientPutWithRateLimit() {
        int numberOfThreads = 8;
        ArrayList<Thread> threadsWithPutRequest = createThreadsWithPutRequest(numberOfThreads);

        Instant startTime = Instant.now();
        threadsWithPutRequest.forEach(Thread::start);

        await().untilAsserted(() -> verify(httpServiceClient, times(numberOfThreads))
                .putWithStatusCode("url", "username", "password", "payload"));

        Instant finishTime = Instant.now();

        Duration processingTime = Duration.between(startTime, finishTime);

        assertThat(processingTime).isCloseTo(Duration.ofSeconds(4), Duration.ofMillis(500));

    }

    private ArrayList<Thread> createThreadsWithGetRequest(int amount) {
        ArrayList<Thread> threadsList = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            Thread thread = new Thread(() -> rateLimitClient.getWithStatusCodeNoRateLimit("url",
                    "username", "password"));
            threadsList.add(thread);
        }
        return threadsList;
    }

    private ArrayList<Thread> createThreadsWithPutRequest(int amount) {
        ArrayList<Thread> threadsList = new ArrayList<>();
        for (int i = 0; i < amount; i++) {
            Thread thread = new Thread(() -> rateLimitClient.putWithStatusCodeWithTwoSecRateLimit("url",
                    "username", "password", "payload"));
            threadsList.add(thread);
        }
        return threadsList;
    }
}