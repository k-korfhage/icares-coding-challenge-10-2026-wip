package de.icares.coding_challenge.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.stream.Stream;

import de.icares.coding_challenge.service.ProviderServiceResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class AppointmentCsvParserTest {

    private final AppointmentCsvParser parser = new AppointmentCsvParser();
    private final Map<String, ProviderServiceResponse> offers = Map.of(
            "DIA", new ProviderServiceResponse(7L, "DIA", "PC-Diagnose", 30));

    @Test
    void parsesValidCsvAndTrimsCustomerNumber() {
        var appointments = parser.parse(
                "KdnNr,APP_DATE,SERVICE\n 10001 ,2026-11-02T09:00:00Z,DIA\n".getBytes(StandardCharsets.UTF_8),
                offers);

        assertEquals(1, appointments.size());
        assertEquals(2, appointments.getFirst().lineNumber());
        assertEquals("10001", appointments.getFirst().customerNumber());
        assertEquals(OffsetDateTime.parse("2026-11-02T09:00:00Z"), appointments.getFirst().start());
        assertEquals(7L, appointments.getFirst().serviceId());
    }

    @Test
    void reportsAllInvalidRowsBeforeReturningAnyAppointments() {
        String csv = "KdnNr,APP_DATE,SERVICE\n"
                + "10001,not-a-date,DIA\n"
                + ",2026-11-02T09:07:00Z,UNKNOWN\n";

        AppointmentImportValidationException exception = assertThrows(AppointmentImportValidationException.class,
                () -> parser.parse(csv.getBytes(StandardCharsets.UTF_8), offers));

        assertEquals(4, exception.getErrors().size());
        assertEquals(2, exception.getErrors().get(0).lineNumber());
        assertEquals(3, exception.getErrors().get(1).lineNumber());
        assertEquals(3, exception.getErrors().get(2).lineNumber());
        assertEquals(3, exception.getErrors().get(3).lineNumber());
    }

    @Test
    void acceptsUtf8BomAndQuotedFields() {
        var appointments = parser.parse(
                "\uFEFFKdnNr,APP_DATE,SERVICE\n\"customer, 10001\",2026-11-02T09:00:00Z,DIA\n"
                        .getBytes(StandardCharsets.UTF_8), offers);

        assertEquals("customer, 10001", appointments.getFirst().customerNumber());
    }

    @ParameterizedTest
    @MethodSource("invalidHeaders")
    void rejectsInvalidHeaders(String header) {
        String csv = header + "\n10001,2026-11-02T09:00:00Z,DIA\n";

        AppointmentImportValidationException exception = assertThrows(AppointmentImportValidationException.class,
                () -> parser.parse(csv.getBytes(StandardCharsets.UTF_8), offers));

        assertEquals(1, exception.getErrors().getFirst().lineNumber());
    }

    @Test
    void acceptsHeaderNamesWithDifferentCaseAndOuterWhitespace() {
        String csv = " kdnnr , app_date , service \n10001,2026-11-02T09:00:00Z,DIA\n";

        var appointments = parser.parse(csv.getBytes(StandardCharsets.UTF_8), offers);

        assertEquals(1, appointments.size());
        assertEquals("10001", appointments.getFirst().customerNumber());
    }

    private static Stream<String> invalidHeaders() {
        return Stream.of(
                "",
                "   ",
                "CustomerNumber,APP_DATE,SERVICE",
                "KdnNr,SERVICE,APP_DATE",
                "KdnNr,APP_DATE",
                "KdnNr,APP_DATE,SERVICE,EXTRA",
                "\"KdnNr,APP_DATE,SERVICE");
    }
}
