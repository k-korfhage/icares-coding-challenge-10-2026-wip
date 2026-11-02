package de.icares.coding_challenge.appointment;

import java.time.OffsetDateTime;

public record AppointmentSuggestionResponse(
        OffsetDateTime start, OffsetDateTime end, Long providerId, Long serviceId) {
}
