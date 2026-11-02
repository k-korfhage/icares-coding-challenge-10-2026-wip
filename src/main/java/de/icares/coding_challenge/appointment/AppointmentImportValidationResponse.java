package de.icares.coding_challenge.appointment;

import java.util.List;

public record AppointmentImportValidationResponse(List<AppointmentImportError> errors) {
}
