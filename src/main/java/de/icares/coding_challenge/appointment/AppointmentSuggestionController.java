package de.icares.coding_challenge.appointment;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/service-providers/{providerId}/appointment-suggestions")
public class AppointmentSuggestionController {

    private final AppointmentSuggestionService suggestionService;

    public AppointmentSuggestionController(AppointmentSuggestionService suggestionService) {
        this.suggestionService = suggestionService;
    }

    @GetMapping
    public List<AppointmentSuggestionResponse> getAppointmentSuggestions(
            @PathVariable Long providerId,
            @RequestParam Long serviceId,
            @RequestParam OffsetDateTime from,
            @RequestParam OffsetDateTime to) {
        return suggestionService.suggest(providerId, serviceId, from, to);
    }
}
