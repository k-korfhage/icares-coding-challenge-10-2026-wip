package de.icares.coding_challenge.provider;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProviderController.class)
class ProviderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProviderRepository providerRepository;

    @Test
    void getProviders_returnsProviderListAsJson() throws Exception {
        Provider provider = provider(1L, "IT-Service Müller", 2);
        given(providerRepository.findAll()).willReturn(List.of(provider));

        mockMvc.perform(get("/service-providers").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].providerId").value(1))
                .andExpect(jsonPath("$[0].name").value("IT-Service Müller"))
                .andExpect(jsonPath("$[0].capacity").value(2))
                .andExpect(jsonPath("$[0].id").doesNotExist());
    }

    @Test
    void getProvider_returnsProviderAsJson() throws Exception {
        Provider provider = provider(1L, "IT-Service Müller", 2);
        given(providerRepository.findById(1L)).willReturn(Optional.of(provider));

        mockMvc.perform(get("/service-providers/1").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.providerId").value(1))
                .andExpect(jsonPath("$.name").value("IT-Service Müller"))
                .andExpect(jsonPath("$.capacity").value(2));
    }

    @Test
    void getUnknownProvider_returnsNotFound() throws Exception {
        given(providerRepository.findById(999L)).willReturn(Optional.empty());

        mockMvc.perform(get("/service-providers/999"))
                .andExpect(status().isNotFound());
    }

    private Provider provider(Long id, String name, Integer capacity) {
        Provider provider = org.mockito.Mockito.mock(Provider.class);
        given(provider.getId()).willReturn(id);
        given(provider.getName()).willReturn(name);
        given(provider.getMaxParallelAppointments()).willReturn(capacity);
        return provider;
    }
}
