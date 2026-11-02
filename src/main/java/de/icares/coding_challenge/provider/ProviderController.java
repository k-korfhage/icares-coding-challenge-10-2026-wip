package de.icares.coding_challenge.provider;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/service-providers")
public class ProviderController {

    private final ProviderRepository providerRepository;

    public ProviderController(ProviderRepository providerRepository) {
        this.providerRepository = providerRepository;
    }

    @GetMapping
    public List<ProviderResponse> getProviders() {
        return providerRepository.findAll().stream()
                .map(ProviderResponse::from)
                .toList();
    }

    @GetMapping("/{providerId}")
    public ProviderResponse getProvider(@PathVariable Long providerId) {
        Provider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "IT-Service-Anbieter nicht gefunden"
                ));

        return ProviderResponse.from(provider);
    }
}
