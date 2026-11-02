package de.icares.coding_challenge.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
class TransactionalAppointmentImporterRollbackTest {

    private static final OffsetDateTime START = OffsetDateTime.parse("2026-11-02T09:00:00Z");

    @Autowired
    private TransactionalAppointmentImporter importer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private AppointmentBookingService bookingService;

    private Long providerId;
    private Long serviceId;
    private Long offerId;

    @BeforeEach
    void createProviderOffer() {
        String suffix = UUID.randomUUID().toString();
        providerId = jdbcTemplate.queryForObject(
                "insert into provider (name, max_parallel_appointments) values (?, 1) returning id",
                Long.class, "import-rollback-provider-" + suffix);
        serviceId = jdbcTemplate.queryForObject(
                "insert into service (service_code, name) values (?, ?) returning id",
                Long.class, "IMPORT-ROLLBACK-" + suffix, "Import rollback test");
        offerId = jdbcTemplate.queryForObject(
                "insert into provider_service (provider_id, service_id, duration_minutes) "
                        + "values (?, ?, 30) returning id",
                Long.class, providerId, serviceId);
    }

    @AfterEach
    void removeFixture() {
        if (providerId != null) {
            jdbcTemplate.update("delete from appointment where provider_service_id in "
                    + "(select id from provider_service where provider_id = ?)", providerId);
            jdbcTemplate.update("delete from provider_service where provider_id = ?", providerId);
            jdbcTemplate.update("delete from provider where id = ?", providerId);
        }
        if (serviceId != null) {
            jdbcTemplate.update("delete from service where id = ?", serviceId);
        }
    }

    @Test
    void rollsBackEarlierRowsWhenBookingPhaseFindsAConflict() {
        AtomicInteger bookingCalls = new AtomicInteger();
        given(bookingService.book(eq(providerId), any())).willAnswer(invocation -> {
            AppointmentRequest request = invocation.getArgument(1);
            if (bookingCalls.incrementAndGet() == 1) {
                jdbcTemplate.update(
                        "insert into appointment (provider_service_id, customer_key, starts_at, ends_at) "
                                + "values (?, ?, ?, ?)",
                        offerId, request.customerNumber(), request.start(), request.start().plusMinutes(30));
                return new AppointmentResponse(1L, request.customerNumber(), request.start(),
                        request.start().plusMinutes(30), providerId, "Test provider", serviceId, "Test service");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        });

        List<ValidatedImportAppointment> rows = List.of(
                new ValidatedImportAppointment(2, "customer-A", START, serviceId, 30, "DIA"),
                new ValidatedImportAppointment(3, "customer-B", START.plusMinutes(30), serviceId, 30, "DIA"));

        assertThrows(AppointmentImportConflictException.class,
                () -> importer.importAppointments(providerId, rows));

        Integer storedAppointments = jdbcTemplate.queryForObject(
                "select count(*) from appointment where provider_service_id = ?", Integer.class, offerId);
        assertEquals(0, storedAppointments);
        assertEquals(2, bookingCalls.get());
    }
}
