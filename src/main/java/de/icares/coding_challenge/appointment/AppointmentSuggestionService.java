package de.icares.coding_challenge.appointment;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import de.icares.coding_challenge.provider.Provider;
import de.icares.coding_challenge.provider.ProviderRepository;
import de.icares.coding_challenge.service.ProviderService;
import de.icares.coding_challenge.service.ProviderServiceRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AppointmentSuggestionService {

    private static final int SLOT_MINUTES = 15;

    private final ProviderRepository providerRepository;
    private final ProviderServiceRepository providerServiceRepository;
    private final AppointmentRepository appointmentRepository;

    public AppointmentSuggestionService(ProviderRepository providerRepository,
            ProviderServiceRepository providerServiceRepository, AppointmentRepository appointmentRepository) {
        this.providerRepository = providerRepository;
        this.providerServiceRepository = providerServiceRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public List<AppointmentSuggestionResponse> suggest(
            Long providerId, Long serviceId, OffsetDateTime from, OffsetDateTime to) {
        if (providerId == null || serviceId == null || from == null || to == null || from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ungültiger Suchzeitraum oder fehlende Angaben");
        }

        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "IT-Service-Anbieter nicht gefunden"));
        ProviderService offer = providerServiceRepository.findOfferByProviderIdAndServiceId(providerId, serviceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Dienstleistung für diesen Anbieter nicht gefunden"));

        int durationMinutes = offer.getDurationMinutes();
        OffsetDateTime firstCandidate = firstSlotAtOrAfter(from);
        if (firstCandidate.isAfter(to)) {
            return List.of();
        }

        OffsetDateTime lastCandidate = firstCandidate;
        while (!lastCandidate.plusMinutes(SLOT_MINUTES).isAfter(to)) {
            lastCandidate = lastCandidate.plusMinutes(SLOT_MINUTES);
        }
        List<Appointment> overlapping = appointmentRepository.findOverlappingAppointments(
                providerId, firstCandidate, lastCandidate.plusMinutes(durationMinutes));

        List<AppointmentSuggestionResponse> suggestions = new ArrayList<>();
        for (OffsetDateTime candidate = firstCandidate; !candidate.isAfter(to);
                candidate = candidate.plusMinutes(SLOT_MINUTES)) {
            OffsetDateTime end = candidate.plusMinutes(durationMinutes);
            if (AppointmentCapacityChecker.hasCapacity(
                    provider.getMaxParallelAppointments(), candidate, end, overlapping)) {
                suggestions.add(new AppointmentSuggestionResponse(candidate, end, providerId, serviceId));
            }
        }
        return suggestions;
    }

    private OffsetDateTime firstSlotAtOrAfter(OffsetDateTime from) {
        OffsetDateTime candidate = from.withSecond(0).withNano(0);
        if (candidate.isBefore(from)) {
            candidate = candidate.plusMinutes(1);
        }
        int minutesToNextSlot = (SLOT_MINUTES - candidate.getMinute() % SLOT_MINUTES) % SLOT_MINUTES;
        return candidate.plusMinutes(minutesToNextSlot);
    }
}
