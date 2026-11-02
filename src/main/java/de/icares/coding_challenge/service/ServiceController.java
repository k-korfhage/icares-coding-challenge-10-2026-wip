package de.icares.coding_challenge.service;

import java.util.List;

import de.icares.coding_challenge.provider.ProviderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/service-providers/{providerId}/services")
public class ServiceController {

    private final ProviderRepository providerRepository;
    private final ProviderServiceRepository providerServiceRepository;

    public ServiceController(ProviderRepository providerRepository,
            ProviderServiceRepository providerServiceRepository) {
        this.providerRepository = providerRepository;
        this.providerServiceRepository = providerServiceRepository;
    }

    @GetMapping
    public List<ProviderServiceResponse> getProviderServices(@PathVariable Long providerId) {
        if (!providerRepository.existsById(providerId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "IT-Service-Anbieter nicht gefunden");
        }

        return providerServiceRepository.findByProviderIdWithService(providerId).stream()
                .map(ProviderServiceResponse::from)
                .toList();
    }
}
