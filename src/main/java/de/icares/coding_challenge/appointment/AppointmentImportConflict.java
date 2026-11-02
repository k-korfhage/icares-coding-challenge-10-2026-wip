package de.icares.coding_challenge.appointment;

import java.time.OffsetDateTime;
import java.util.List;

public record AppointmentImportConflict(
        int lineNumber,
        String customerNumber,
        String serviceCode,
        OffsetDateTime start,
        String reason,
        boolean conflictsWithExistingAppointments,
        List<Integer> overlappingAcceptedCsvLineNumbers) {
}
