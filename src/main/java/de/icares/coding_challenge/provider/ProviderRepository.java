package de.icares.coding_challenge.provider;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProviderRepository extends JpaRepository<Provider, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select provider from Provider provider where provider.id = :providerId")
    Optional<Provider> findByIdForUpdate(@Param("providerId") Long providerId);
}
