package de.icares.coding_challenge.appointment;

import java.time.OffsetDateTime;
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
public class AppointmentBookingService {

    private final ProviderRepository providerRepository;
    private final ProviderServiceRepository providerServiceRepository;
    private final AppointmentRepository appointmentRepository;

    public AppointmentBookingService(ProviderRepository providerRepository,
            ProviderServiceRepository providerServiceRepository,
            AppointmentRepository appointmentRepository) {
        this.providerRepository = providerRepository;
        this.providerServiceRepository = providerServiceRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional
    public AppointmentResponse book(Long providerId, AppointmentRequest request) {
        validateRequest(request);

        // Lock this provider until commit so concurrent bookings cannot both pass the capacity check.
        Provider provider = providerRepository.findByIdForUpdate(providerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "IT-Service-Anbieter nicht gefunden"));

        ProviderService offer = providerServiceRepository
                .findOfferByProviderIdAndServiceId(providerId, request.serviceId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Dienstleistung für diesen Anbieter nicht gefunden"));

        OffsetDateTime start = request.start();
        OffsetDateTime end = start.plusMinutes(offer.getDurationMinutes());

        List<Appointment> overlapping = appointmentRepository
                .findOverlappingAppointments(providerId, start, end);
        if (!AppointmentCapacityChecker.hasCapacity(
                provider.getMaxParallelAppointments(), start, end, overlapping)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Termin nicht verfügbar oder Kapazität ausgeschöpft");
        }

        Appointment appointment = new Appointment(offer, request.customerNumber().trim(), start, end);
        return AppointmentResponse.from(appointmentRepository.save(appointment));
    }

    private void validateRequest(AppointmentRequest request) {
        if (request == null || request.customerNumber() == null || request.customerNumber().isBlank()
                || request.start() == null || request.serviceId() == null || request.serviceId() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ungültige Buchungsdaten");
        }

        OffsetDateTime start = request.start();
        if (start.getMinute() % 15 != 0 || start.getSecond() != 0 || start.getNano() != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Startzeit muss im 15-Minuten-Raster liegen");
        }
    }

}
