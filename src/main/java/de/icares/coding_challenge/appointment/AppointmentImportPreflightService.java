package de.icares.coding_challenge.appointment;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import de.icares.coding_challenge.provider.Provider;
import de.icares.coding_challenge.provider.ProviderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AppointmentImportPreflightService {

    private final ProviderRepository providerRepository;
    private final AppointmentRepository appointmentRepository;

    public AppointmentImportPreflightService(
            ProviderRepository providerRepository, AppointmentRepository appointmentRepository) {
        this.providerRepository = providerRepository;
        this.appointmentRepository = appointmentRepository;
    }

    @Transactional(readOnly = true)
    public List<AppointmentImportConflict> findCapacityConflicts(
            Long providerId, List<ValidatedImportAppointment> rows) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "IT-Service-Anbieter nicht gefunden"));

        OffsetDateTime earliestStart = rows.stream().map(ValidatedImportAppointment::start)
                .min(OffsetDateTime::compareTo).orElseThrow();
        OffsetDateTime latestEnd = rows.stream().map(ValidatedImportAppointment::end)
                .max(OffsetDateTime::compareTo).orElseThrow();
        List<Appointment> existingAppointments = appointmentRepository.findOverlappingAppointments(
                providerId, earliestStart, latestEnd);

        List<Appointment> provisionalSchedule = new ArrayList<>(existingAppointments);
        List<ValidatedImportAppointment> acceptedCsvRows = new ArrayList<>();
        List<AppointmentImportConflict> conflicts = new ArrayList<>();
        for (ValidatedImportAppointment row : rows) {
            if (AppointmentCapacityChecker.hasCapacity(provider.getMaxParallelAppointments(),
                    row.start(), row.end(), provisionalSchedule)) {
                provisionalSchedule.add(new Appointment(null, row.customerNumber(), row.start(), row.end()));
                acceptedCsvRows.add(row);
            } else {
                boolean conflictsWithExisting = !AppointmentCapacityChecker.hasCapacity(
                        provider.getMaxParallelAppointments(), row.start(), row.end(), existingAppointments);
                // Overlaps with previously rejected CSV rows are not included in the list of import-internal overlaps.
                List<Integer> overlappingAcceptedCsvLines = acceptedCsvRows.stream()
                        .filter(accepted -> accepted.start().isBefore(row.end()) && accepted.end().isAfter(row.start()))
                        .map(ValidatedImportAppointment::lineNumber)
                        .toList();
                conflicts.add(new AppointmentImportConflict(row.lineNumber(), row.customerNumber(), row.serviceCode(),
                        row.start(), "Anbieterkapazität wird während des Termins überschritten",
                        conflictsWithExisting, overlappingAcceptedCsvLines));
            }
        }
        return List.copyOf(conflicts);
    }
}
