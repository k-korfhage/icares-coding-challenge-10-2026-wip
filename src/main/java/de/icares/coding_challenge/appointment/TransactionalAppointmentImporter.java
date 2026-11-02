package de.icares.coding_challenge.appointment;

import java.util.ArrayList;
import java.util.List;

import de.icares.coding_challenge.provider.ProviderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TransactionalAppointmentImporter {

    private final ProviderRepository providerRepository;
    private final AppointmentBookingService bookingService;

    public TransactionalAppointmentImporter(ProviderRepository providerRepository,
            AppointmentBookingService bookingService) {
        this.providerRepository = providerRepository;
        this.bookingService = bookingService;
    }

    @Transactional
    public List<AppointmentResponse> importAppointments(
            Long providerId, List<ValidatedImportAppointment> rows) {
        providerRepository.findByIdForUpdate(providerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "IT-Service-Anbieter nicht gefunden"));

        List<AppointmentResponse> importedAppointments = new ArrayList<>();
        for (ValidatedImportAppointment row : rows) {
            try {
                importedAppointments.add(bookingService.book(providerId,
                        new AppointmentRequest(row.customerNumber(), row.start(), row.serviceId())));
            } catch (ResponseStatusException exception) {
                if (exception.getStatusCode().value() == HttpStatus.CONFLICT.value()) {
                    throw new AppointmentImportConflictException(List.of(new AppointmentImportConflict(
                            row.lineNumber(), row.customerNumber(), row.serviceCode(), row.start(),
                            "Kapazitätskonflikt nach erfolgreicher Vorabprüfung",
                            true, List.of())));
                }
                throw exception;
            }
        }
        return List.copyOf(importedAppointments);
    }
}
