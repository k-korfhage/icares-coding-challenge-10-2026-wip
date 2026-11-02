package de.icares.coding_challenge.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProviderServiceRepository extends JpaRepository<ProviderService, Long> {

    @Query("""
            select providerService
            from ProviderService providerService
            join fetch providerService.service
            where providerService.provider.id = :providerId
            order by providerService.service.code
            """)
    List<ProviderService> findByProviderIdWithService(@Param("providerId") Long providerId);

    @Query("""
            select providerService
            from ProviderService providerService
            join fetch providerService.service
            where providerService.provider.id = :providerId
              and providerService.service.id = :serviceId
            """)
    Optional<ProviderService> findOfferByProviderIdAndServiceId(
            @Param("providerId") Long providerId,
            @Param("serviceId") Long serviceId);
}
