package de.icares.coding_challenge.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import de.icares.coding_challenge.provider.Provider;
import de.icares.coding_challenge.provider.ProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class TransactionalAppointmentImporterTest {

    private static final OffsetDateTime START = OffsetDateTime.parse("2026-11-02T09:00:00Z");

    private final ProviderRepository providerRepository = mock(ProviderRepository.class);
    private final AppointmentBookingService bookingService = mock(AppointmentBookingService.class);

    private TransactionalAppointmentImporter importer;
    private Provider provider;

    @BeforeEach
    void setUp() {
        importer = new TransactionalAppointmentImporter(providerRepository, bookingService);
        provider = mock(Provider.class);
        given(providerRepository.findByIdForUpdate(1L)).willReturn(Optional.of(provider));
        given(bookingService.book(eq(1L), any())).willAnswer(invocation -> {
            AppointmentRequest request = invocation.getArgument(1);
            return new AppointmentResponse(100L, request.customerNumber(), request.start(),
                    request.start().plusMinutes(30), 1L, "Provider", 7L, "DIA");
        });
    }

    @Test
    void booksRowsInCsvOrderInsideTheTransaction() {
        List<ValidatedImportAppointment> rows = List.of(
                row(2, START, "customer-A"),
                row(3, START.plusMinutes(30), "customer-B"));

        List<AppointmentResponse> response = importer.importAppointments(1L, rows);

        assertEquals(2, response.size());
        verify(bookingService).book(1L, new AppointmentRequest("customer-A", START, 7L));
        verify(bookingService).book(1L,
                new AppointmentRequest("customer-B", START.plusMinutes(30), 7L));
    }

    @Test
    void reportsBookingPhaseConflictAsImportConflict() {
        given(bookingService.book(eq(1L), any()))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT));
        List<ValidatedImportAppointment> rows = List.of(row(2, START, "customer-A"));

        AppointmentImportConflictException exception = assertThrows(AppointmentImportConflictException.class,
                () -> importer.importAppointments(1L, rows));

        assertEquals(2, exception.getConflicts().getFirst().lineNumber());
        assertEquals("customer-A", exception.getConflicts().getFirst().customerNumber());
    }

    private ValidatedImportAppointment row(int line, OffsetDateTime start, String customer) {
        return new ValidatedImportAppointment(line, customer, start, 7L, 30, "DIA");
    }
}
