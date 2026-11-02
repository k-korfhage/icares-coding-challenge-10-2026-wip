package de.icares.coding_challenge.appointment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import de.icares.coding_challenge.provider.Provider;
import de.icares.coding_challenge.provider.ProviderRepository;
import de.icares.coding_challenge.service.ProviderService;
import de.icares.coding_challenge.service.ProviderServiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class AppointmentSuggestionServiceTest {

    private static final OffsetDateTime START = OffsetDateTime.parse("2026-11-02T09:00:00Z");

    private final ProviderRepository providerRepository = mock(ProviderRepository.class);
    private final ProviderServiceRepository providerServiceRepository = mock(ProviderServiceRepository.class);
    private final AppointmentRepository appointmentRepository = mock(AppointmentRepository.class);

    private AppointmentSuggestionService suggestionService;
    private Provider provider;
    private ProviderService offer;

    @BeforeEach
    void setUp() {
        suggestionService = new AppointmentSuggestionService(
                providerRepository, providerServiceRepository, appointmentRepository);
        provider = mock(Provider.class);
        offer = mock(ProviderService.class);
        given(providerRepository.findById(1L)).willReturn(Optional.of(provider));
        given(provider.getMaxParallelAppointments()).willReturn(1);
        given(providerServiceRepository.findOfferByProviderIdAndServiceId(1L, 7L))
                .willReturn(Optional.of(offer));
        given(offer.getDurationMinutes()).willReturn(30);
        given(appointmentRepository.findOverlappingAppointments(1L, START, START.plusMinutes(75)))
                .willReturn(List.of());
    }

    @Test
    void suggestsGridAlignedStartsAtOrAfterFromAndCalculatesEndFromServiceDuration() {
        List<AppointmentSuggestionResponse> suggestions = suggestionService.suggest(
                1L, 7L, START.plusMinutes(7), START.plusMinutes(45));

        assertEquals(List.of(START.plusMinutes(15), START.plusMinutes(30), START.plusMinutes(45)),
                suggestions.stream().map(AppointmentSuggestionResponse::start).toList());
        assertEquals(START.plusMinutes(75), suggestions.getLast().end());
        assertEquals(1L, suggestions.getFirst().providerId());
        assertEquals(7L, suggestions.getFirst().serviceId());
    }

    @Test
    void checksCapacityAcrossTheEntireSuggestedServiceDuration() {
        Appointment existing = new Appointment(null, "existing", START.plusMinutes(30), START.plusMinutes(60));
        given(appointmentRepository.findOverlappingAppointments(1L, START, START.plusMinutes(90)))
                .willReturn(List.of(existing));

        List<AppointmentSuggestionResponse> suggestions = suggestionService.suggest(
                1L, 7L, START, START.plusMinutes(60));

        assertEquals(List.of(START, START.plusMinutes(60)),
                suggestions.stream().map(AppointmentSuggestionResponse::start).toList());
    }

    @Test
    void returnsBadRequestForReversedSearchRange() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> suggestionService.suggest(1L, 7L, START.plusMinutes(15), START));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void returnsNotFoundForUnknownProvider() {
        given(providerRepository.findById(999L)).willReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> suggestionService.suggest(999L, 7L, START, START.plusMinutes(30)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void returnsNotFoundWhenProviderDoesNotOfferRequestedService() {
        given(providerServiceRepository.findOfferByProviderIdAndServiceId(1L, 999L))
                .willReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> suggestionService.suggest(1L, 999L, START, START.plusMinutes(30)));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }
}
