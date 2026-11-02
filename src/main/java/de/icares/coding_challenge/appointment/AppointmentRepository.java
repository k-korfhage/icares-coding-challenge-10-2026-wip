package de.icares.coding_challenge.appointment;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("""
            select appointment
            from Appointment appointment
            join fetch appointment.providerService offer
            join fetch offer.provider
            join fetch offer.service
            where offer.provider.id = :providerId and appointment.id = :appointmentId
            """)
    Optional<Appointment> findByProviderIdAndId(
            @Param("providerId") Long providerId,
            @Param("appointmentId") Long appointmentId);

    @Query("""
            select appointment
            from Appointment appointment
            where appointment.providerService.provider.id = :providerId
              and appointment.start < :end
              and appointment.end > :start
            """)
    List<Appointment> findOverlappingAppointments(
            @Param("providerId") Long providerId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end);
}
