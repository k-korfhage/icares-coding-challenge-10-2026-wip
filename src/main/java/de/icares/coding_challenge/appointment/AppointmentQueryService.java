package de.icares.coding_challenge.appointment;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Fetch;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentQueryService {

    private final EntityManager entityManager;

    public AppointmentQueryService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // Unlike provider and service reads, this endpoint has several optional filters. Building only the
    // active predicates avoids untyped NULL parameters in PostgreSQL and keeps one query for all combinations.
    @Transactional(readOnly = true)
    public List<Appointment> findByProviderIdWithFilters(
            Long providerId, OffsetDateTime from, OffsetDateTime to, Long serviceId, String customerNumber) {
        var builder = entityManager.getCriteriaBuilder();
        var query = builder.createQuery(Appointment.class);
        var appointment = query.from(Appointment.class);
        Fetch<Appointment, de.icares.coding_challenge.service.ProviderService> offerFetch =
                appointment.fetch("providerService", JoinType.INNER);
        offerFetch.fetch("provider", JoinType.INNER);
        offerFetch.fetch("service", JoinType.INNER);
        Join<Appointment, de.icares.coding_challenge.service.ProviderService> offer =
                appointment.join("providerService", JoinType.INNER);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(builder.equal(offer.get("provider").get("id"), providerId));
        if (from != null) {
            predicates.add(builder.greaterThanOrEqualTo(appointment.get("start"), from));
        }
        if (to != null) {
            predicates.add(builder.lessThanOrEqualTo(appointment.get("start"), to));
        }
        if (serviceId != null) {
            predicates.add(builder.equal(offer.get("service").get("id"), serviceId));
        }
        if (customerNumber != null) {
            predicates.add(builder.equal(appointment.get("customerNumber"), customerNumber));
        }

        query.select(appointment).where(predicates.toArray(Predicate[]::new))
                .orderBy(builder.asc(appointment.get("start")));
        return entityManager.createQuery(query).getResultList();
    }
}
