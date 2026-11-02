package de.icares.coding_challenge.appointment;

import java.time.OffsetDateTime;

import de.icares.coding_challenge.provider.Provider;
import de.icares.coding_challenge.service.ProviderService;
import de.icares.coding_challenge.service.Service;

public record AppointmentResponse(
        Long id,
        String customerNumber,
        OffsetDateTime start,
        OffsetDateTime end,
        Long providerId,
        String providerName,
        Long serviceId,
        String serviceName) {

    public static AppointmentResponse from(Appointment appointment) {
        ProviderService offer = appointment.getProviderService();
        Provider provider = offer.getProvider();
        Service service = offer.getService();
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getCustomerNumber(),
                appointment.getStart(),
                appointment.getEnd(),
                provider.getId(),
                provider.getName(),
                service.getId(),
                service.getName());
    }
}
