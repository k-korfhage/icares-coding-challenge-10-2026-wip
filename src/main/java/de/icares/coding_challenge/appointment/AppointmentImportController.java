package de.icares.coding_challenge.appointment;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/service-providers/{providerId}/appointments/import")
public class AppointmentImportController {

    private final AppointmentImportService importService;

    public AppointmentImportController(AppointmentImportService importService) {
        this.importService = importService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importAppointments(
            @PathVariable Long providerId, @RequestPart("file") MultipartFile file) {
        try {
            var importedAppointments = importService.importCsv(providerId, file);
            var location = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/service-providers/{providerId}/appointments/{appointmentId}")
                    .buildAndExpand(providerId, importedAppointments.getFirst().id())
                    .toUri();
            return ResponseEntity.created(location).body(importedAppointments);
        } catch (AppointmentImportValidationException exception) {
            return ResponseEntity.badRequest()
                    .body(new AppointmentImportValidationResponse(exception.getErrors()));
        } catch (AppointmentImportConflictException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new AppointmentImportConflictResponse(exception.getConflicts()));
        } catch (ResponseStatusException exception) {
            return ResponseEntity.status(exception.getStatusCode()).build();
        }
    }
}
