package de.icares.coding_challenge.appointment;

import java.util.List;

public class AppointmentImportConflictException extends RuntimeException {

    private final List<AppointmentImportConflict> conflicts;

    public AppointmentImportConflictException(List<AppointmentImportConflict> conflicts) {
        super("CSV-Import wegen Kapazitätskonflikten abgebrochen");
        this.conflicts = List.copyOf(conflicts);
    }

    public List<AppointmentImportConflict> getConflicts() {
        return conflicts;
    }
}
