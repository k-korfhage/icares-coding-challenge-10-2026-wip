package de.icares.coding_challenge.appointment;

import java.time.OffsetDateTime;

public record AppointmentRequest(String customerNumber, OffsetDateTime start, Long serviceId) {
}
