package de.icares.coding_challenge.appointment;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import de.icares.coding_challenge.provider.ProviderRepository;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/service-providers/{providerId}/appointments")
public class AppointmentController {

    private final AppointmentBookingService bookingService;
    private final AppointmentQueryService appointmentQueryService;
    private final AppointmentRepository appointmentRepository;
    private final ProviderRepository providerRepository;

    public AppointmentController(AppointmentBookingService bookingService,
            AppointmentQueryService appointmentQueryService, AppointmentRepository appointmentRepository,
            ProviderRepository providerRepository) {
        this.bookingService = bookingService;
        this.appointmentQueryService = appointmentQueryService;
        this.appointmentRepository = appointmentRepository;
        this.providerRepository = providerRepository;
    }

    @GetMapping
    public List<AppointmentResponse> getAppointments(@PathVariable Long providerId,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) String customerNumber) {
        requireProvider(providerId);
        if (from != null && to != null && from.isAfter(to)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ungültiger Abfragezeitraum");
        }
        return appointmentQueryService.findByProviderIdWithFilters(providerId, from, to, serviceId,
                        customerNumber == null || customerNumber.isBlank() ? null : customerNumber)
                .stream().map(AppointmentResponse::from).toList();
    }

    @GetMapping("/{appointmentId}")
    public AppointmentResponse getAppointment(@PathVariable Long providerId, @PathVariable Long appointmentId) {
        requireProvider(providerId);
        return appointmentRepository.findByProviderIdAndId(providerId, appointmentId)
                .map(AppointmentResponse::from)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Termin nicht gefunden"));
    }

    private void requireProvider(Long providerId) {
        if (!providerRepository.existsById(providerId)) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.NOT_FOUND, "IT-Service-Anbieter nicht gefunden");
        }
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(
            @PathVariable Long providerId,
            @RequestBody AppointmentRequest request) {
        AppointmentResponse response = bookingService.book(providerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
