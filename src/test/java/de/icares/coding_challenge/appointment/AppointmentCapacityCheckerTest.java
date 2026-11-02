package de.icares.coding_challenge.appointment;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class AppointmentCapacityCheckerTest {

    private static final OffsetDateTime START = OffsetDateTime.parse("2026-11-02T09:00:00Z");

    @Test
    void acceptsFirstAppointmentInFreePeriod() {
        assertTrue(AppointmentCapacityChecker.hasCapacity(1, START, START.plusMinutes(30), List.of()));
    }

    @Test
    void allowsOverlapUpToProviderCapacity() {
        Appointment existing = appointment(START, START.plusMinutes(30));

        assertTrue(AppointmentCapacityChecker.hasCapacity(
                2, START, START.plusMinutes(30), List.of(existing)));
    }

    @Test
    void rejectsBookingThatWouldExceedCapacity() {
        Appointment first = appointment(START, START.plusMinutes(30));
        Appointment second = appointment(START, START.plusMinutes(30));

        assertFalse(AppointmentCapacityChecker.hasCapacity(
                2, START, START.plusMinutes(30), List.of(first, second)));
    }

    @Test
    void checksCapacityAcrossTheWholeRequestedInterval() {
        Appointment first = appointment(START, START.plusMinutes(30));
        Appointment second = appointment(START.plusMinutes(15), START.plusMinutes(45));

        assertFalse(AppointmentCapacityChecker.hasCapacity(
                2, START, START.plusMinutes(45), List.of(first, second)));
    }

    @Test
    void allowsAppointmentsThatOnlyTouchAtAnEndpoint() {
        Appointment existing = appointment(START.minusMinutes(30), START);

        assertTrue(AppointmentCapacityChecker.hasCapacity(
                1, START, START.plusMinutes(30), List.of(existing)));
    }

    @Test
    void countsAppointmentsThatPartiallyOverlapTheRequestedPeriod() {
        Appointment existing = appointment(START.minusMinutes(15), START.plusMinutes(15));

        assertFalse(AppointmentCapacityChecker.hasCapacity(
                1, START, START.plusMinutes(30), List.of(existing)));
    }

    private Appointment appointment(OffsetDateTime start, OffsetDateTime end) {
        return new Appointment(null, "existing-customer", start, end);
    }
}
