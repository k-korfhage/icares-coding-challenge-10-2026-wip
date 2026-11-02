package de.icares.coding_challenge.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
class AppointmentImportIntegrationTest {

    private static final OffsetDateTime FIRST_START = OffsetDateTime.parse("2026-11-02T09:00:00Z");
    private static final OffsetDateTime SECOND_START = OffsetDateTime.parse("2026-11-02T09:30:00Z");

    @Autowired
    private AppointmentImportService importService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long providerId;
    private Long firstServiceId;
    private Long secondServiceId;
    private String firstServiceCode;
    private String secondServiceCode;

    @BeforeEach
    void createProviderAndServiceOffers() {
        String suffix = UUID.randomUUID().toString();
        providerId = jdbcTemplate.queryForObject(
                "insert into provider (name, max_parallel_appointments) values (?, 1) returning id",
                Long.class, "import-success-provider-" + suffix);
        firstServiceCode = "IMPORT-DIA-" + suffix;
        secondServiceCode = "IMPORT-LONG-" + suffix;
        firstServiceId = insertService(firstServiceCode, "Import diagnosis", 30);
        secondServiceId = insertService(secondServiceCode, "Import long service", 60);
    }

    @AfterEach
    void removeTestData() {
        if (providerId != null) {
            jdbcTemplate.update("delete from appointment where provider_service_id in "
                    + "(select id from provider_service where provider_id = ?)", providerId);
            jdbcTemplate.update("delete from provider_service where provider_id = ?", providerId);
            jdbcTemplate.update("delete from provider where id = ?", providerId);
        }
        if (firstServiceId != null && secondServiceId != null) {
            jdbcTemplate.update("delete from service where id in (?, ?)", firstServiceId, secondServiceId);
        }
    }

    @Test
    void importsAllValidRowsPersistsAppointmentsAndReturnsTheirDetails() {
        String csv = "KdnNr,APP_DATE,SERVICE\n"
                + "customer-A,2026-11-02T09:00:00Z," + firstServiceCode + "\n"
                + "customer-B,2026-11-02T09:30:00Z," + secondServiceCode + "\n";
        MockMultipartFile file = new MockMultipartFile(
                "file", "appointments.csv", "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        List<AppointmentResponse> response = importService.importCsv(providerId, file);

        assertEquals(2, response.size());
        AppointmentResponse first = response.get(0);
        AppointmentResponse second = response.get(1);
        assertNotNull(first.id());
        assertNotNull(second.id());
        assertEquals("customer-A", first.customerNumber());
        assertEquals(FIRST_START, first.start());
        assertEquals(FIRST_START.plusMinutes(30), first.end());
        assertEquals(providerId, first.providerId());
        assertEquals(firstServiceId, first.serviceId());
        assertEquals("Import diagnosis", first.serviceName());
        assertEquals("customer-B", second.customerNumber());
        assertEquals(SECOND_START, second.start());
        assertEquals(SECOND_START.plusMinutes(60), second.end());
        assertEquals(providerId, second.providerId());
        assertEquals(secondServiceId, second.serviceId());
        assertEquals("Import long service", second.serviceName());

        Integer storedAppointments = jdbcTemplate.queryForObject(
                "select count(*) from appointment a join provider_service ps "
                        + "on ps.id = a.provider_service_id where ps.provider_id = ?",
                Integer.class, providerId);
        assertEquals(2, storedAppointments);
    }

    private Long insertService(String code, String name, int durationMinutes) {
        Long serviceId = jdbcTemplate.queryForObject(
                "insert into service (service_code, name) values (?, ?) returning id",
                Long.class, code, name);
        jdbcTemplate.update(
                "insert into provider_service (provider_id, service_id, duration_minutes) values (?, ?, ?)",
                providerId, serviceId, durationMinutes);
        return serviceId;
    }

}
