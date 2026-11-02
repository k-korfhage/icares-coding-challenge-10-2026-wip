package de.icares.coding_challenge.appointment;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class AppointmentCapacityChecker {

    private AppointmentCapacityChecker() {
    }

    static boolean hasCapacity(int capacity, OffsetDateTime start, OffsetDateTime end,
            List<Appointment> existingAppointments) {
        List<CapacityEvent> events = new ArrayList<>();
        for (Appointment appointment : existingAppointments) {
            if (!appointment.getStart().isBefore(end) || !appointment.getEnd().isAfter(start)) {
                continue;
            }
            OffsetDateTime clippedStart = appointment.getStart().isBefore(start) ? start : appointment.getStart();
            OffsetDateTime clippedEnd = appointment.getEnd().isAfter(end) ? end : appointment.getEnd();
            events.add(new CapacityEvent(clippedStart, 1));
            events.add(new CapacityEvent(clippedEnd, -1));
        }

        // Include the requested booking in the same capacity sweep.
        events.add(new CapacityEvent(start, 1));
        events.add(new CapacityEvent(end, -1));
        events.sort(Comparator.comparing(CapacityEvent::time).thenComparingInt(CapacityEvent::change));

        int active = 0;
        for (CapacityEvent event : events) {
            // End events (-1) precede starts (+1), so adjacent [start, end) bookings do not overlap.
            active += event.change();
            if (active > capacity) {
                return false;
            }
        }
        return true;
    }

    private record CapacityEvent(OffsetDateTime time, int change) {
    }
}
