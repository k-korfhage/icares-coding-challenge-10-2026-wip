package de.icares.coding_challenge.service;

import de.icares.coding_challenge.provider.Provider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "provider_service")
public class ProviderService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private Provider provider;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private Service service;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    protected ProviderService() {
    }

    public Long getId() {
        return id;
    }

    public Provider getProvider() {
        return provider;
    }

    public Service getService() {
        return service;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }
}
