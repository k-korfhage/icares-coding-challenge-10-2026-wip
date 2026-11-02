package de.icares.coding_challenge.provider;

public record ProviderResponse(Long providerId, String name, Integer capacity) {

    public static ProviderResponse from(Provider provider) {
        return new ProviderResponse(
                provider.getId(),
                provider.getName(),
                provider.getMaxParallelAppointments()
        );
    }
}
