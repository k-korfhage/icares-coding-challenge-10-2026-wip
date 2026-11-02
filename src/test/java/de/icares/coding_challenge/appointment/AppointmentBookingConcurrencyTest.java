package de.icares.coding_challenge.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class AppointmentBookingConcurrencyTest {

    private static final Logger log = LoggerFactory.getLogger(AppointmentBookingConcurrencyTest.class);

    private static final OffsetDateTime APPOINTMENT_START =
            OffsetDateTime.parse("2026-11-02T09:00:00Z");

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long providerId;
    private Long serviceId;

    @BeforeEach
    void createIsolatedProviderAndService() {
        providerId = jdbcTemplate.queryForObject(
                "insert into provider (name, max_parallel_appointments) values (?, 1) returning id",
                Long.class,
                "concurrency-test-" + UUID.randomUUID());
        serviceId = jdbcTemplate.queryForObject(
                "insert into service (service_code, name) values (?, ?) returning id",
                Long.class,
                "TEST-" + UUID.randomUUID(), "Concurrency test service");
        jdbcTemplate.update(
                "insert into provider_service (provider_id, service_id, duration_minutes) values (?, ?, 30)",
                providerId, serviceId);
        log.info("Created concurrency-test data in database 'test': providerId={}, serviceId={}",
                providerId, serviceId);
    }

    @AfterEach
    void removeTestData() {
        if (providerId == null) {
            return;
        }
        jdbcTemplate.update("delete from appointment where provider_service_id in "
                + "(select id from provider_service where provider_id = ?)", providerId);
        jdbcTemplate.update("delete from provider_service where provider_id = ?", providerId);
        jdbcTemplate.update("delete from provider where id = ?", providerId);
        if (serviceId != null) {
            jdbcTemplate.update("delete from service where id = ?", serviceId);
        }
        log.info("Removed concurrency-test data: providerId={}, serviceId={}, and all associated instances", providerId, serviceId);
    }

    @Test
    void simultaneousBookingsForLastCapacitySlotAllowOnlyOneRequest() throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch startTogether = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        Future<ResponseEntity<String>> first = null;
        Future<ResponseEntity<String>> second = null;
        try {
            configureHttpTimeouts();
            first = executor.submit(() -> sendBooking(ready, startTogether, "parallel-1"));
            second = executor.submit(() -> sendBooking(ready, startTogether, "parallel-2"));

            if (!ready.await(10, TimeUnit.SECONDS)) {
                throw new AssertionError("Beide Buchungs-Threads wurden nicht rechtzeitig bereit");
            }
            startTogether.countDown();

            ResponseEntity<String> firstResponse = first.get(20, TimeUnit.SECONDS);
            ResponseEntity<String> secondResponse = second.get(20, TimeUnit.SECONDS);
            HttpStatus firstStatus = HttpStatus.valueOf(firstResponse.getStatusCode().value());
            HttpStatus secondStatus = HttpStatus.valueOf(secondResponse.getStatusCode().value());
            log.info("parallel-1 response: status={}, body={}", firstStatus, firstResponse.getBody());
            log.info("parallel-2 response: status={}, body={}", secondStatus, secondResponse.getBody());
            assertEquals(1, (firstStatus == HttpStatus.CREATED ? 1 : 0)
                    + (secondStatus == HttpStatus.CREATED ? 1 : 0));
            assertEquals(1, (firstStatus == HttpStatus.CONFLICT ? 1 : 0)
                    + (secondStatus == HttpStatus.CONFLICT ? 1 : 0));

            Integer storedAppointments = jdbcTemplate.queryForObject(
                    "select count(*) from appointment a join provider_service ps "
                            + "on ps.id = a.provider_service_id where ps.provider_id = ?",
                    Integer.class, providerId);
            assertEquals(1, storedAppointments);
            log.info("Number of stored appointments for providerId={}: {}", providerId, storedAppointments);
        } finally {
            startTogether.countDown();
            cancelIfRunning(first);
            cancelIfRunning(second);
            executor.shutdownNow();
            if (!executor.awaitTermination(12, TimeUnit.SECONDS)) {
                throw new AssertionError("Buchungs-Threads wurden nach dem Abbruch nicht beendet");
            }
        }
    }

    private void configureHttpTimeouts() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(8));
        restTemplate.getRestTemplate().setRequestFactory(requestFactory);
    }

    private void cancelIfRunning(Future<?> future) {
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }

    private ResponseEntity<String> sendBooking(CountDownLatch ready, CountDownLatch startTogether,
            String customerNumber)
            throws InterruptedException {
        ready.countDown();
        if (!startTogether.await(10, TimeUnit.SECONDS)) {
            throw new AssertionError("Buchungs-Threads wurden nicht gemeinsam gestartet");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of(
                "customerNumber", customerNumber,
                "start", APPOINTMENT_START.toString(),
                "serviceId", serviceId);
        return restTemplate.postForEntity(
                "http://localhost:" + port + "/service-providers/" + providerId + "/appointments",
                new HttpEntity<>(body, headers), String.class);
    }
}
