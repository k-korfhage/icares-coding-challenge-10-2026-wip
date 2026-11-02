package de.icares.coding_challenge.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** Exercises the Criteria query against PostgreSQL, including Hibernate fetch behavior. */
@SpringBootTest
class AppointmentQueryServiceTest {

    private static final OffsetDateTime AT_09 = OffsetDateTime.parse("2026-11-02T09:00:00Z");
    private static final OffsetDateTime AT_10 = OffsetDateTime.parse("2026-11-02T10:00:00Z");
    private static final OffsetDateTime AT_11 = OffsetDateTime.parse("2026-11-02T11:00:00Z");

    @Autowired
    private AppointmentQueryService appointmentQueryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long providerId;
    private Long otherProviderId;
    private Long serviceId;
    private Long otherServiceId;
    private Long serviceWithoutAppointmentsId;

    @BeforeEach
    void createAppointmentFixture() {
        String suffix = UUID.randomUUID().toString();
        providerId = insertProvider("query-test-provider-" + suffix);
        otherProviderId = insertProvider("query-test-other-provider-" + suffix);
        serviceId = insertService("QUERY-" + suffix, "Query test service");
        otherServiceId = insertService("QUERY-OTHER-" + suffix, "Other query test service");
        serviceWithoutAppointmentsId = insertService("QUERY-EMPTY-" + suffix, "Service without appointments");

        long providerOfferId = insertOffer(providerId, serviceId);
        long otherProviderOfferId = insertOffer(otherProviderId, serviceId);
        long otherServiceOfferId = insertOffer(providerId, otherServiceId);
        insertOffer(providerId, serviceWithoutAppointmentsId);
        insertAppointment(providerOfferId, "customer-A", AT_09);
        insertAppointment(providerOfferId, "customer-B", AT_10);
        insertAppointment(otherServiceOfferId, "customer-C", AT_11);
        insertAppointment(otherProviderOfferId, "customer-B", AT_10);
    }

    @AfterEach
    void removeAppointmentFixture() {
        // If fixture setup fails partway through, these guards may skip cleanup for already-created rows.
        if (providerId == null || otherProviderId == null) {
            return;
        }
        jdbcTemplate.update("delete from appointment where provider_service_id in "
                + "(select id from provider_service where provider_id in (?, ?))", providerId, otherProviderId);
        jdbcTemplate.update("delete from provider_service where provider_id in (?, ?)", providerId, otherProviderId);
        jdbcTemplate.update("delete from provider where id in (?, ?)", providerId, otherProviderId);
        if (serviceId != null && otherServiceId != null && serviceWithoutAppointmentsId != null) {
            jdbcTemplate.update("delete from service where id in (?, ?, ?)",
                    serviceId, otherServiceId, serviceWithoutAppointmentsId);
        }
    }

    @Test
    void returnsOnlyProviderAppointmentsInStartTimeOrderAndFetchesResponseAssociations() {
        List<Appointment> appointments = appointmentQueryService.findByProviderIdWithFilters(
                providerId, null, null, null, null);

        assertEquals(3, appointments.size());
        assertEquals(List.of(AT_09, AT_10, AT_11), appointments.stream().map(Appointment::getStart).toList());
        assertTrue(appointments.getFirst().getProviderService().getProvider().getName()
                .startsWith("query-test-provider-"));
        assertEquals("Query test service", appointments.getFirst().getProviderService().getService().getName());
    }

    @Test
    void appliesAllOptionalFiltersTogether() {
        List<Appointment> appointments = appointmentQueryService.findByProviderIdWithFilters(
                providerId, AT_10, AT_11, otherServiceId, "customer-C");

        assertEquals(1, appointments.size());
        assertEquals("customer-C", appointments.getFirst().getCustomerNumber());
        assertEquals(AT_11, appointments.getFirst().getStart());
    }

    @Test
    void appliesFromFilterWithoutOtherOptionalFilters() {
        List<Appointment> appointments = appointmentQueryService.findByProviderIdWithFilters(
                providerId, AT_10, null, null, null);

        assertEquals(List.of(AT_10, AT_11), appointments.stream().map(Appointment::getStart).toList());
    }

    @Test
    void appliesToFilterWithoutOtherOptionalFilters() {
        List<Appointment> appointments = appointmentQueryService.findByProviderIdWithFilters(
                providerId, null, AT_10, null, null);

        assertEquals(List.of(AT_09, AT_10), appointments.stream().map(Appointment::getStart).toList());
    }

    @Test
    void appliesServiceFilterWithoutOtherOptionalFilters() {
        List<Appointment> appointments = appointmentQueryService.findByProviderIdWithFilters(
                providerId, null, null, otherServiceId, null);

        assertEquals(1, appointments.size());
        assertEquals("customer-C", appointments.getFirst().getCustomerNumber());
        assertEquals(AT_11, appointments.getFirst().getStart());
    }

    @Test
    void appliesCustomerNumberFilterWithoutOtherOptionalFilters() {
        List<Appointment> appointments = appointmentQueryService.findByProviderIdWithFilters(
                providerId, null, null, null, "customer-B");

        assertEquals(1, appointments.size());
        assertEquals(AT_10, appointments.getFirst().getStart());
    }

    @Test
    void includesAppointmentsStartingExactlyAtFromAndTo() {
        List<Appointment> appointments = appointmentQueryService.findByProviderIdWithFilters(
                providerId, AT_10, AT_11, null, null);

        assertEquals(List.of(AT_10, AT_11), appointments.stream().map(Appointment::getStart).toList());
    }

    @Test
    void returnsEmptyListWhenNoAppointmentsMatch() {
        List<Appointment> appointments = appointmentQueryService.findByProviderIdWithFilters(
                providerId, null, null, null, "no-such-customer");

        assertTrue(appointments.isEmpty());
    }

    @Test
    void returnsEmptyListWhenServiceDoesNotMatch() {
        List<Appointment> appointments = appointmentQueryService.findByProviderIdWithFilters(
                providerId, null, null, serviceWithoutAppointmentsId, null);

        assertTrue(appointments.isEmpty());
    }

    @Test
    void returnsEmptyListForUnknownProvider() {
        List<Appointment> appointments = appointmentQueryService.findByProviderIdWithFilters(
                Long.MAX_VALUE, null, null, null, null);

        assertTrue(appointments.isEmpty());
    }

    private Long insertProvider(String name) {
        return jdbcTemplate.queryForObject(
                "insert into provider (name, max_parallel_appointments) values (?, 2) returning id",
                Long.class, name);
    }

    private Long insertService(String code, String name) {
        return jdbcTemplate.queryForObject(
                "insert into service (service_code, name) values (?, ?) returning id",
                Long.class, code, name);
    }

    private Long insertOffer(Long provider, Long service) {
        return jdbcTemplate.queryForObject(
                "insert into provider_service (provider_id, service_id, duration_minutes) values (?, ?, 30) returning id",
                Long.class, provider, service);
    }

    private void insertAppointment(Long offer, String customerNumber, OffsetDateTime start) {
        jdbcTemplate.update(
                "insert into appointment (provider_service_id, customer_key, starts_at, ends_at) values (?, ?, ?, ?)",
                offer, customerNumber, start, start.plusMinutes(30));
    }
}
