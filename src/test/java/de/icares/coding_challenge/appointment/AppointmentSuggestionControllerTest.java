package de.icares.coding_challenge.appointment;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(AppointmentSuggestionController.class)
class AppointmentSuggestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentSuggestionService suggestionService;

    @Test
    void returnsSuggestionsAsJson() throws Exception {
        OffsetDateTime start = OffsetDateTime.parse("2026-11-02T09:00:00Z");
        given(suggestionService.suggest(1L, 7L, start, start.plusHours(1)))
                .willReturn(List.of(new AppointmentSuggestionResponse(
                        start, start.plusMinutes(30), 1L, 7L)));

        mockMvc.perform(get("/service-providers/1/appointment-suggestions")
                        .param("serviceId", "7")
                        .param("from", "2026-11-02T09:00:00Z")
                        .param("to", "2026-11-02T10:00:00Z")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].start").value("2026-11-02T09:00:00Z"))
                .andExpect(jsonPath("$[0].end").value("2026-11-02T09:30:00Z"))
                .andExpect(jsonPath("$[0].providerId").value(1))
                .andExpect(jsonPath("$[0].serviceId").value(7));
    }

    @Test
    void returnsBadRequestWhenRequiredQueryParameterIsMissing() throws Exception {
        mockMvc.perform(get("/service-providers/1/appointment-suggestions")
                        .param("serviceId", "7")
                        .param("from", "2026-11-02T09:00:00Z"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mapsSuggestionServiceErrorsToTheirHttpStatuses() throws Exception {
        OffsetDateTime start = OffsetDateTime.parse("2026-11-02T09:00:00Z");
        given(suggestionService.suggest(1L, 7L, start, start.plusHours(1)))
                .willThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/service-providers/1/appointment-suggestions")
                        .param("serviceId", "7")
                        .param("from", "2026-11-02T09:00:00Z")
                        .param("to", "2026-11-02T10:00:00Z"))
                .andExpect(status().isNotFound());
    }
}
