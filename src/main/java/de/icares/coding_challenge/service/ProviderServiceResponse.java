package de.icares.coding_challenge.service;

public record ProviderServiceResponse(Long serviceId, String code, String name, Integer durationMinutes) {

    public static ProviderServiceResponse from(ProviderService providerService) {
        Service service = providerService.getService();
        return new ProviderServiceResponse(
                service.getId(),
                service.getCode(),
                service.getName(),
                providerService.getDurationMinutes()
        );
    }
}
