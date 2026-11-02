package de.icares.coding_challenge.appointment;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.icares.coding_challenge.provider.ProviderRepository;
import de.icares.coding_challenge.service.ProviderService;
import de.icares.coding_challenge.service.ProviderServiceRepository;
import de.icares.coding_challenge.service.ProviderServiceResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AppointmentImportService {

    private final ProviderRepository providerRepository;
    private final ProviderServiceRepository providerServiceRepository;
    private final AppointmentCsvParser csvParser;
    private final AppointmentImportPreflightService preflightService;
    private final TransactionalAppointmentImporter transactionalImporter;

    public AppointmentImportService(ProviderRepository providerRepository,
            ProviderServiceRepository providerServiceRepository, AppointmentCsvParser csvParser,
            AppointmentImportPreflightService preflightService,
            TransactionalAppointmentImporter transactionalImporter) {
        this.providerRepository = providerRepository;
        this.providerServiceRepository = providerServiceRepository;
        this.csvParser = csvParser;
        this.preflightService = preflightService;
        this.transactionalImporter = transactionalImporter;
    }

    public List<AppointmentResponse> importCsv(Long providerId, MultipartFile file) {
        if (providerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "providerId fehlt");
        }
        providerRepository.findById(providerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "IT-Service-Anbieter nicht gefunden"));
        if (file == null || file.isEmpty()) {
            throw new AppointmentImportValidationException(
                    List.of(new AppointmentImportError(1, "CSV-Datei ist leer oder fehlt")));
        }

        Map<String, ProviderServiceResponse> offersByCode = new HashMap<>();
        for (ProviderService offer : providerServiceRepository.findByProviderIdWithService(providerId)) {
            ProviderServiceResponse response = ProviderServiceResponse.from(offer);
            offersByCode.put(response.code(), response);
        }

        try {
            List<ValidatedImportAppointment> rows = csvParser.parse(file.getBytes(), offersByCode);
            List<AppointmentImportConflict> conflicts = preflightService.findCapacityConflicts(providerId, rows);
            if (!conflicts.isEmpty()) {
                throw new AppointmentImportConflictException(conflicts);
            }
            return transactionalImporter.importAppointments(providerId, rows);
        } catch (java.io.IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CSV-Datei konnte nicht gelesen werden");
        }
    }
}
