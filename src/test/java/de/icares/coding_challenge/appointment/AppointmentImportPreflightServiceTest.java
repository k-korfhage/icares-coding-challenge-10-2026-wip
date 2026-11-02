package de.icares.coding_challenge.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import de.icares.coding_challenge.provider.Provider;
import de.icares.coding_challenge.provider.ProviderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AppointmentImportPreflightServiceTest {

    private static final OffsetDateTime START = OffsetDateTime.parse("2026-11-02T09:00:00Z");

    private final ProviderRepository providerRepository = mock(ProviderRepository.class);
    private final AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);

    private AppointmentImportPreflightService preflightService;
    private Provider provider;

    @BeforeEach
    void setUp() {
        preflightService = new AppointmentImportPreflightService(providerRepository, appointmentRepository);
        provider = mock(Provider.class);
        given(providerRepository.findById(1L)).willReturn(Optional.of(provider));
        given(provider.getMaxParallelAppointments()).willReturn(1);
        given(appointmentRepository.findOverlappingAppointments(eq(1L), any(), any())).willReturn(List.of());
    }

    @Test
    void collectsAllCsvConflictsBeforeAnyBookingOrProviderLock() {
        List<ValidatedImportAppointment> rows = List.of(
                row(2, START, "customer-A"),
                row(3, START, "customer-B"),
                row(4, START, "customer-C"));

        List<AppointmentImportConflict> conflicts = preflightService.findCapacityConflicts(1L, rows);

        assertEquals(List.of(3, 4), conflicts.stream().map(AppointmentImportConflict::lineNumber).toList());
        assertEquals(List.of(List.of(2), List.of(2)),
                conflicts.stream().map(AppointmentImportConflict::overlappingAcceptedCsvLineNumbers).toList());
        verify(providerRepository).findById(1L);
        verify(providerRepository, never()).findByIdForUpdate(1L);
    }

    @Test
    void reportsRowsConflictingWithAlreadyStoredAppointments() {
        Appointment existing = new Appointment(null, "existing", START, START.plusMinutes(60));
        given(appointmentRepository.findOverlappingAppointments(eq(1L), any(), any()))
                .willReturn(List.of(existing));
        List<ValidatedImportAppointment> rows = List.of(
                row(2, START.plusMinutes(15), "customer-A"),
                row(3, START.plusMinutes(30), "customer-B"));

        List<AppointmentImportConflict> conflicts = preflightService.findCapacityConflicts(1L, rows);

        assertEquals(List.of(2, 3), conflicts.stream().map(AppointmentImportConflict::lineNumber).toList());
        assertEquals(List.of(true, true),
                conflicts.stream().map(AppointmentImportConflict::conflictsWithExistingAppointments).toList());
    }

    private ValidatedImportAppointment row(int line, OffsetDateTime start, String customer) {
        return new ValidatedImportAppointment(line, customer, start, 7L, 30, "DIA");
    }
}
