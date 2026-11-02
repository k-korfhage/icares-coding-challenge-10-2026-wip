package de.icares.coding_challenge.appointment;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import de.icares.coding_challenge.provider.Provider;
import de.icares.coding_challenge.provider.ProviderRepository;
import de.icares.coding_challenge.service.ProviderService;
import de.icares.coding_challenge.service.ProviderServiceRepository;
import de.icares.coding_challenge.service.Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class AppointmentImportServiceTest {

    private final ProviderRepository providerRepository = mock(ProviderRepository.class);
    private final ProviderServiceRepository providerServiceRepository = mock(ProviderServiceRepository.class);
    private final AppointmentImportPreflightService preflightService = mock(AppointmentImportPreflightService.class);
    private final TransactionalAppointmentImporter transactionalImporter = mock(TransactionalAppointmentImporter.class);

    private AppointmentImportService importService;

    @BeforeEach
    void setUp() {
        importService = new AppointmentImportService(providerRepository, providerServiceRepository,
                new AppointmentCsvParser(), preflightService, transactionalImporter);
        given(providerRepository.findById(1L)).willReturn(Optional.of(mock(Provider.class)));
        ProviderService offer = mock(ProviderService.class);
        Service service = mock(Service.class);
        given(providerServiceRepository.findByProviderIdWithService(1L)).willReturn(List.of(offer));
        given(offer.getService()).willReturn(service);
        given(offer.getDurationMinutes()).willReturn(30);
        given(service.getId()).willReturn(7L);
        given(service.getCode()).willReturn("DIA");
        given(service.getName()).willReturn("PC-Diagnose");
    }

    @Test
    void runsCapacityPreflightBeforeStartingTransactionalBooking() {
        given(preflightService.findCapacityConflicts(eq(1L), anyList())).willReturn(List.of());
        given(transactionalImporter.importAppointments(eq(1L), anyList()))
                .willReturn(List.of());

        importService.importCsv(1L, csv());

        var order = inOrder(preflightService, transactionalImporter);
        order.verify(preflightService).findCapacityConflicts(eq(1L), anyList());
        order.verify(transactionalImporter).importAppointments(eq(1L), anyList());
    }

    @Test
    void doesNotStartTransactionalBookingWhenPreflightFindsConflicts() {
        AppointmentImportConflict conflict = new AppointmentImportConflict(
                2, "customer-A", "DIA", OffsetDateTime.parse("2026-11-02T09:00:00Z"),
                "Capacity exceeded", true, List.of());
        given(preflightService.findCapacityConflicts(eq(1L), anyList())).willReturn(List.of(conflict));

        assertThrows(AppointmentImportConflictException.class, () -> importService.importCsv(1L, csv()));

        verify(transactionalImporter, never()).importAppointments(eq(1L), anyList());
    }

    @Test
    void rejectsEmptyFileBeforeLoadingOffersOrStartingImport() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.csv", "text/csv", new byte[0]);

        AppointmentImportValidationException exception = assertThrows(AppointmentImportValidationException.class,
                () -> importService.importCsv(1L, emptyFile));

        assertEquals(1, exception.getErrors().size());
        assertEquals(1, exception.getErrors().getFirst().lineNumber());
        verify(providerServiceRepository, never()).findByProviderIdWithService(1L);
        verify(preflightService, never()).findCapacityConflicts(eq(1L), anyList());
        verify(transactionalImporter, never()).importAppointments(eq(1L), anyList());
    }

    @Test
    void rejectsMissingFileBeforeLoadingOffersOrStartingImport() {
        AppointmentImportValidationException exception = assertThrows(AppointmentImportValidationException.class,
                () -> importService.importCsv(1L, null));

        assertEquals(1, exception.getErrors().size());
        assertEquals(1, exception.getErrors().getFirst().lineNumber());
        verify(providerServiceRepository, never()).findByProviderIdWithService(1L);
        verify(preflightService, never()).findCapacityConflicts(eq(1L), anyList());
        verify(transactionalImporter, never()).importAppointments(eq(1L), anyList());
    }

    private MockMultipartFile csv() {
        return new MockMultipartFile("file", "appointments.csv", "text/csv",
                "KdnNr,APP_DATE,SERVICE\ncustomer-A,2026-11-02T09:00:00Z,DIA\n".getBytes());
    }
}
