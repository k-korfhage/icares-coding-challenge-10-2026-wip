package de.icares.coding_challenge.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import de.icares.coding_challenge.provider.ProviderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ServiceController.class)
class ServiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProviderRepository providerRepository;

    @MockitoBean
    private ProviderServiceRepository providerServiceRepository;

    @Test
    void getProviderServices_returnsProviderServicesAsJson() throws Exception {
        ProviderService providerService = mock(ProviderService.class);
        Service service = mock(Service.class);

        given(providerRepository.existsById(1L)).willReturn(true);
        given(providerServiceRepository.findByProviderIdWithService(1L))
                .willReturn(List.of(providerService));
        given(providerService.getService()).willReturn(service);
        given(providerService.getDurationMinutes()).willReturn(30);
        given(service.getId()).willReturn(6L);
        given(service.getCode()).willReturn("DIA");
        given(service.getName()).willReturn("PC-Diagnose");

        mockMvc.perform(get("/service-providers/1/services").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].serviceId").value(6))
                .andExpect(jsonPath("$[0].code").value("DIA"))
                .andExpect(jsonPath("$[0].name").value("PC-Diagnose"))
                .andExpect(jsonPath("$[0].durationMinutes").value(30))
                .andExpect(jsonPath("$[0].id").doesNotExist());
    }

    @Test
    void getProviderServices_returnsEmptyListWhenProviderHasNoServices() throws Exception {
        given(providerRepository.existsById(1L)).willReturn(true);
        given(providerServiceRepository.findByProviderIdWithService(1L)).willReturn(List.of());

        mockMvc.perform(get("/service-providers/1/services").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]"));
    }

    @Test
    void getProviderServices_returnsNotFoundForUnknownProvider() throws Exception {
        given(providerRepository.existsById(999L)).willReturn(false);

        mockMvc.perform(get("/service-providers/999/services").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
