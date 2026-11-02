package de.icares.coding_challenge.appointment;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AppointmentImportController.class)
class AppointmentImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppointmentImportService importService;

    @Test
    void returnsImportedAppointmentsOnSuccess() throws Exception {
        OffsetDateTime start = OffsetDateTime.parse("2026-11-02T09:00:00Z");
        given(importService.importCsv(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .willReturn(List.of(new AppointmentResponse(
                        101L, "10001", start, start.plusMinutes(30), 1L, "Provider", 7L, "PC-Diagnose")));

        mockMvc.perform(multipart("/service-providers/1/appointments/import")
                        .file(new MockMultipartFile("file", "appointments.csv", "text/csv",
                                "KdnNr,APP_DATE,SERVICE\n10001,2026-11-02T09:00:00Z,DIA\n".getBytes()))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(101))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
                        .string("Location", org.hamcrest.Matchers.endsWith(
                                "/service-providers/1/appointments/101")));
    }

    @Test
    void returnsAllValidationErrorsWithBadRequest() throws Exception {
        given(importService.importCsv(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .willThrow(new AppointmentImportValidationException(List.of(
                        new AppointmentImportError(2, "Ungültiger Zeitpunkt"),
                        new AppointmentImportError(4, "Unbekannter Service-Code"))));

        mockMvc.perform(multipart("/service-providers/1/appointments/import")
                        .file(new MockMultipartFile("file", "appointments.csv", "text/csv", new byte[] {1})))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].lineNumber").value(2))
                .andExpect(jsonPath("$.errors[1].lineNumber").value(4));
    }

    @Test
    void returnsAllCapacityConflictsWithConflictStatus() throws Exception {
        given(importService.importCsv(org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any()))
                .willThrow(new AppointmentImportConflictException(List.of(new AppointmentImportConflict(
                        3, "10002", "DIA", OffsetDateTime.parse("2026-11-02T09:00:00Z"),
                        "Kapazität überschritten", false, List.of(2)))));

        mockMvc.perform(multipart("/service-providers/1/appointments/import")
                        .file(new MockMultipartFile("file", "appointments.csv", "text/csv", new byte[] {1})))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.conflicts[0].lineNumber").value(3))
                .andExpect(jsonPath("$.conflicts[0].overlappingAcceptedCsvLineNumbers[0]").value(2));
    }
}
