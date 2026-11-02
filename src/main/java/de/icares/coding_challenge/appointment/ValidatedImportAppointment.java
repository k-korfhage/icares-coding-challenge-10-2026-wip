package de.icares.coding_challenge.appointment;

import java.time.OffsetDateTime;

record ValidatedImportAppointment(
        int lineNumber, String customerNumber, OffsetDateTime start, Long serviceId,
        int durationMinutes, String serviceCode) {

    OffsetDateTime end() {
        return start.plusMinutes(durationMinutes);
    }
}
