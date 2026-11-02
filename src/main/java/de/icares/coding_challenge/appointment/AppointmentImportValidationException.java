package de.icares.coding_challenge.appointment;

import java.util.List;

public class AppointmentImportValidationException extends RuntimeException {

    private final List<AppointmentImportError> errors;

    public AppointmentImportValidationException(List<AppointmentImportError> errors) {
        super("CSV-Datei enthält ungültige Zeilen");
        this.errors = List.copyOf(errors);
    }

    public List<AppointmentImportError> getErrors() {
        return errors;
    }
}
