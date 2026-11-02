package de.icares.coding_challenge.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** Verifies suggestion results against persisted appointments in PostgreSQL. */
@SpringBootTest
class AppointmentSuggestionTest {

    private static final OffsetDateTime FROM = OffsetDateTime.parse("2026-11-02T09:00:00Z");
    private static final OffsetDateTime TO = OffsetDateTime.parse("2026-11-02T10:00:00Z");

    @Autowired
    private AppointmentSuggestionService suggestionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long providerId;
    private Long serviceId;

    @BeforeEach
    void createProviderServiceAndExistingAppointment() {
        String suffix = UUID.randomUUID().toString();
        providerId = jdbcTemplate.queryForObject(
                "insert into provider (name, max_parallel_appointments) values (?, 1) returning id",
                Long.class, "suggestion-test-provider-" + suffix);
        serviceId = jdbcTemplate.queryForObject(
                "insert into service (service_code, name) values (?, ?) returning id",
                Long.class, "SUGGEST-" + suffix, "Suggestion test service");
        Long offerId = jdbcTemplate.queryForObject(
                "insert into provider_service (provider_id, service_id, duration_minutes) "
                        + "values (?, ?, 30) returning id",
                Long.class, providerId, serviceId);
        jdbcTemplate.update(
                "insert into appointment (provider_service_id, customer_key, starts_at, ends_at) "
                        + "values (?, ?, ?, ?)",
                offerId, "existing-customer", FROM.plusMinutes(30), FROM.plusMinutes(60));
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
    }

    @Test
    void excludesSlotsThatWouldOverlapAnExistingAppointment() {
        List<AppointmentSuggestionResponse> suggestions = suggestionService.suggest(
                providerId, serviceId, FROM, TO);

        assertEquals(List.of(FROM, TO), suggestions.stream().map(AppointmentSuggestionResponse::start).toList());
    }
}
