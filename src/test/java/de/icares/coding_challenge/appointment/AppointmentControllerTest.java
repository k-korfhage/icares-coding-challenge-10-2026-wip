package de.icares.coding_challenge.appointment;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import de.icares.coding_challenge.provider.Provider;
import de.icares.coding_challenge.provider.ProviderRepository;
import de.icares.coding_challenge.service.ProviderService;
import de.icares.coding_challenge.service.Service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(AppointmentController.class)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentBookingService bookingService;

    @MockitoBean
    private AppointmentRepository appointmentRepository;

    @MockitoBean
    private AppointmentQueryService appointmentQueryService;

    @MockitoBean
    private ProviderRepository providerRepository;

    @Test
    void getAppointmentsReturnsFilteredProviderAppointmentsAsJson() throws Exception {
        Appointment appointment = appointment(101L);
        given(providerRepository.existsById(1L)).willReturn(true);
        given(appointmentQueryService.findByProviderIdWithFilters(1L, null, null, null, null))
                .willReturn(List.of(appointment));

        mockMvc.perform(get("/service-providers/1/appointments").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(jsonPath("$[0].customerNumber").value("customer-123"))
                .andExpect(jsonPath("$[0].providerId").value(1))
                .andExpect(jsonPath("$[0].serviceId").value(7));
    }

    @Test
    void getAppointmentsPassesAllQueryFiltersToQueryService() throws Exception {
        given(providerRepository.existsById(1L)).willReturn(true);

        mockMvc.perform(get("/service-providers/1/appointments")
                        .param("from", "2026-11-02T08:00:00Z")
                        .param("to", "2026-11-02T18:00:00Z")
                        .param("serviceId", "7")
                        .param("customerNumber", "customer-123")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(appointmentQueryService).findByProviderIdWithFilters(
                1L,
                OffsetDateTime.parse("2026-11-02T08:00:00Z"),
                OffsetDateTime.parse("2026-11-02T18:00:00Z"),
                7L,
                "customer-123");
    }

    @Test
    void getAppointmentsReturnsNotFoundForUnknownProvider() throws Exception {
        given(providerRepository.existsById(999L)).willReturn(false);

        mockMvc.perform(get("/service-providers/999/appointments").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verifyNoInteractions(appointmentQueryService);
    }

    @Test
    void getAppointmentsReturnsBadRequestWhenFromIsAfterTo() throws Exception {
        given(providerRepository.existsById(1L)).willReturn(true);

        mockMvc.perform(get("/service-providers/1/appointments")
                        .param("from", "2026-11-02T18:00:00Z")
                        .param("to", "2026-11-02T08:00:00Z"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(appointmentQueryService);
    }

    @Test
    void getAppointmentReturnsDetailsAsJson() throws Exception {
        Appointment appointment = appointment(101L);
        given(providerRepository.existsById(1L)).willReturn(true);
        given(appointmentRepository.findByProviderIdAndId(1L, 101L))
                .willReturn(Optional.of(appointment));

        mockMvc.perform(get("/service-providers/1/appointments/101").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.customerNumber").value("customer-123"))
                .andExpect(jsonPath("$.providerName").value("Provider One"))
                .andExpect(jsonPath("$.serviceName").value("PC-Diagnose"));
    }

    @Test
    void getAppointmentReturnsNotFoundForUnknownProviderOrAppointment() throws Exception {
        given(providerRepository.existsById(999L)).willReturn(false);
        mockMvc.perform(get("/service-providers/999/appointments/101"))
                .andExpect(status().isNotFound());

        given(providerRepository.existsById(1L)).willReturn(true);
        given(appointmentRepository.findByProviderIdAndId(1L, 999L)).willReturn(Optional.empty());
        mockMvc.perform(get("/service-providers/1/appointments/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAppointmentReturnsNotFoundWhenAppointmentBelongsToAnotherProvider() throws Exception {
        given(providerRepository.existsById(1L)).willReturn(true);
        given(appointmentRepository.findByProviderIdAndId(1L, 101L)).willReturn(Optional.empty());

        mockMvc.perform(get("/service-providers/1/appointments/101").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    private Appointment appointment(Long id) {
        Appointment appointment = org.mockito.Mockito.mock(Appointment.class);
        ProviderService offer = org.mockito.Mockito.mock(ProviderService.class);
        Provider provider = org.mockito.Mockito.mock(Provider.class);
        Service service = org.mockito.Mockito.mock(Service.class);
        OffsetDateTime start = OffsetDateTime.parse("2026-11-02T09:00:00Z");
        given(appointment.getId()).willReturn(id);
        given(appointment.getCustomerNumber()).willReturn("customer-123");
        given(appointment.getStart()).willReturn(start);
        given(appointment.getEnd()).willReturn(start.plusMinutes(30));
        given(appointment.getProviderService()).willReturn(offer);
        given(offer.getProvider()).willReturn(provider);
        given(offer.getService()).willReturn(service);
        given(provider.getId()).willReturn(1L);
        given(provider.getName()).willReturn("Provider One");
        given(service.getId()).willReturn(7L);
        given(service.getName()).willReturn("PC-Diagnose");
        return appointment;
    }

    @Test
    void createAppointmentReturns201AndAppointmentJson() throws Exception {
        OffsetDateTime start = OffsetDateTime.parse("2026-11-02T09:00:00Z");
        AppointmentResponse response = new AppointmentResponse(
                101L, "customer-123", start, start.plusMinutes(30),
                1L, "Provider One", 7L, "PC-Diagnose");
        given(bookingService.book(1L,
                new AppointmentRequest("customer-123", start, 7L))).willReturn(response);

        mockMvc.perform(post("/service-providers/1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerNumber":"customer-123","start":"2026-11-02T09:00:00Z","serviceId":7}
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(101))
                .andExpect(jsonPath("$.customerNumber").value("customer-123"))
                .andExpect(jsonPath("$.start").value("2026-11-02T09:00:00Z"))
                .andExpect(jsonPath("$.end").value("2026-11-02T09:30:00Z"))
                .andExpect(jsonPath("$.providerId").value(1))
                .andExpect(jsonPath("$.serviceId").value(7));
    }

    @Test
    void createAppointmentMapsBookingErrorsToTheirHttpStatuses() throws Exception {
        given(bookingService.book(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .willThrow(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT));

        mockMvc.perform(post("/service-providers/1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"customerNumber":"customer-123","start":"2026-11-02T09:00:00Z","serviceId":7}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void createAppointmentReturns400WhenRequestJsonIsMalformed() throws Exception {
        mockMvc.perform(post("/service-providers/1/appointments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{customerNumber:customer-123}"))
                .andExpect(status().isBadRequest());
    }
}
