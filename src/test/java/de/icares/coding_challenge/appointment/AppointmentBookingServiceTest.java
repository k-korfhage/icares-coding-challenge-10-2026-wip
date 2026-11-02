package de.icares.coding_challenge.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

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
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AppointmentBookingServiceTest {

    private static final OffsetDateTime START = OffsetDateTime.parse("2026-11-02T09:00:00Z");

    private final ProviderRepository providerRepository = mock(ProviderRepository.class);
    private final ProviderServiceRepository providerServiceRepository = mock(ProviderServiceRepository.class);
    private final AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);

    private AppointmentBookingService bookingService;
    private Provider provider;
    private ProviderService offer;
    private Service service;

    @BeforeEach
    void setUp() {
        bookingService = new AppointmentBookingService(
                providerRepository, providerServiceRepository, appointmentRepository);
        provider = mock(Provider.class);
        offer = mock(ProviderService.class);
        service = mock(Service.class);

        given(providerRepository.findByIdForUpdate(1L)).willReturn(Optional.of(provider));
        given(provider.getMaxParallelAppointments()).willReturn(2);
        given(provider.getId()).willReturn(1L);
        given(provider.getName()).willReturn("Provider One");
        given(providerServiceRepository.findOfferByProviderIdAndServiceId(1L, 7L))
                .willReturn(Optional.of(offer));
        given(offer.getDurationMinutes()).willReturn(30);
        given(offer.getProvider()).willReturn(provider);
        given(offer.getService()).willReturn(service);
        given(service.getId()).willReturn(7L);
        given(service.getName()).willReturn("PC-Diagnose");
        given(appointmentRepository.findOverlappingAppointments(1L, START, START.plusMinutes(30)))
                .willReturn(List.of());
        given(appointmentRepository.save(any(Appointment.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void bookUsesProviderLockBeforeCapacityQueryAndPersistsCalculatedInterval() {
        AppointmentResponse response = bookingService.book(1L,
                new AppointmentRequest(" customer-123 ", START, 7L));

        ArgumentCaptor<Appointment> appointmentCaptor = ArgumentCaptor.forClass(Appointment.class);
        org.mockito.Mockito.verify(appointmentRepository).save(appointmentCaptor.capture());
        Appointment saved = appointmentCaptor.getValue();
        assertEquals("customer-123", saved.getCustomerNumber());
        assertEquals(START, saved.getStart());
        assertEquals(START.plusMinutes(30), saved.getEnd());
        assertEquals(1L, response.providerId());
        assertEquals(7L, response.serviceId());

        InOrder order = inOrder(providerRepository, appointmentRepository);
        order.verify(providerRepository).findByIdForUpdate(1L);
        order.verify(appointmentRepository).findOverlappingAppointments(1L, START, START.plusMinutes(30));
        order.verify(appointmentRepository).save(any(Appointment.class));
    }

    @Test
    void bookRejectsStartOutsideFifteenMinuteGrid() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> bookingService.book(1L, new AppointmentRequest("customer", START.plusMinutes(7), 7L)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        org.mockito.Mockito.verifyNoInteractions(providerRepository, providerServiceRepository, appointmentRepository);
    }

    @Test
    void bookRejectsBlankCustomerNumber() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> bookingService.book(1L, new AppointmentRequest("  ", START, 7L)));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void bookReturnsNotFoundWhenProviderDoesNotExist() {
        given(providerRepository.findByIdForUpdate(999L)).willReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> bookingService.book(999L, new AppointmentRequest("customer", START, 7L)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void bookReturnsNotFoundWhenProviderDoesNotOfferService() {
        given(providerServiceRepository.findOfferByProviderIdAndServiceId(1L, 999L))
                .willReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> bookingService.book(1L, new AppointmentRequest("customer", START, 999L)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void bookReturnsConflictWhenCapacityIsFull() {
        Appointment first = new Appointment(offer, "first", START, START.plusMinutes(30));
        Appointment second = new Appointment(offer, "second", START, START.plusMinutes(30));
        given(appointmentRepository.findOverlappingAppointments(1L, START, START.plusMinutes(30)))
                .willReturn(List.of(first, second));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> bookingService.book(1L, new AppointmentRequest("customer", START, 7L)));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        org.mockito.Mockito.verify(appointmentRepository, org.mockito.Mockito.never())
                .save(any(Appointment.class));
    }
}
