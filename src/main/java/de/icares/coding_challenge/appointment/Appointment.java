package de.icares.coding_challenge.appointment;

import java.time.OffsetDateTime;

import de.icares.coding_challenge.service.ProviderService;
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
@Table(name = "appointment")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "provider_service_id", nullable = false)
    private ProviderService providerService;

    @Column(name = "customer_key", nullable = false)
    private String customerNumber;

    @Column(name = "starts_at", nullable = false)
    private OffsetDateTime start;

    @Column(name = "ends_at", nullable = false)
    private OffsetDateTime end;

    protected Appointment() {
    }

    public Appointment(ProviderService providerService, String customerNumber,
            OffsetDateTime start, OffsetDateTime end) {
        this.providerService = providerService;
        this.customerNumber = customerNumber;
        this.start = start;
        this.end = end;
    }

    public Long getId() {
        return id;
    }

    public ProviderService getProviderService() {
        return providerService;
    }

    public String getCustomerNumber() {
        return customerNumber;
    }

    public OffsetDateTime getStart() {
        return start;
    }

    public OffsetDateTime getEnd() {
        return end;
    }
}
