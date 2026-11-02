package de.icares.coding_challenge.appointment;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.icares.coding_challenge.service.ProviderServiceResponse;
import org.springframework.stereotype.Component;

@Component
public class AppointmentCsvParser {

    public List<ValidatedImportAppointment> parse(byte[] csv, Map<String, ProviderServiceResponse> servicesByCode) {
        String content = decodeUtf8(csv);
        String[] lines = content.split("\\R", -1);
        if (lines.length == 0 || lines[0].isBlank()) {
            throw new AppointmentImportValidationException(
                    List.of(new AppointmentImportError(1, "CSV-Header fehlt")));
        }

        List<String> header;
        try {
            header = parseRecord(stripBom(lines[0]));
        } catch (IllegalArgumentException exception) {
            throw new AppointmentImportValidationException(
                    List.of(new AppointmentImportError(1, "CSV-Header ist fehlerhaft: " + exception.getMessage())));
        }
        if (header.size() != 3 || !header.get(0).trim().equalsIgnoreCase("KdnNr")
                || !header.get(1).trim().equalsIgnoreCase("APP_DATE")
                || !header.get(2).trim().equalsIgnoreCase("SERVICE")) {
            throw new AppointmentImportValidationException(List.of(new AppointmentImportError(
                    1, "Erwarteter Header: KdnNr,APP_DATE,SERVICE")));
        }

        List<ValidatedImportAppointment> appointments = new ArrayList<>();
        List<AppointmentImportError> errors = new ArrayList<>();
        for (int index = 1; index < lines.length; index++) {
            int lineNumber = index + 1;
            String line = lines[index];
            if (line.isBlank()) {
                continue;
            }

            List<String> fields;
            try {
                fields = parseRecord(line);
            } catch (IllegalArgumentException exception) {
                errors.add(new AppointmentImportError(lineNumber,
                        "Ungültiges CSV-Format: " + exception.getMessage()));
                continue;
            }
            if (fields.size() != 3) {
                errors.add(new AppointmentImportError(lineNumber,
                        "Es werden genau drei Spalten erwartet"));
                continue;
            }

            String customerNumber = fields.get(0).trim();
            String dateText = fields.get(1).trim();
            String serviceCode = fields.get(2).trim();
            boolean valid = true;
            if (customerNumber.isBlank()) {
                errors.add(new AppointmentImportError(lineNumber, "KdnNr darf nicht leer sein"));
                valid = false;
            }

            OffsetDateTime start = null;
            try {
                start = OffsetDateTime.parse(dateText);
                if (start.getMinute() % 15 != 0 || start.getSecond() != 0 || start.getNano() != 0) {
                    errors.add(new AppointmentImportError(lineNumber,
                            "APP_DATE muss im 15-Minuten-Raster liegen und darf keine Sekunden enthalten"));
                    valid = false;
                }
            } catch (RuntimeException exception) {
                errors.add(new AppointmentImportError(lineNumber,
                        "APP_DATE muss ein ISO-8601-Zeitpunkt mit Zeitzone sein"));
                valid = false;
            }

            ProviderServiceResponse offer = servicesByCode.get(serviceCode);
            if (serviceCode.isBlank() || offer == null) {
                errors.add(new AppointmentImportError(lineNumber,
                        serviceCode.isBlank() ? "SERVICE darf nicht leer sein"
                                : "Unbekannter oder bei diesem Anbieter nicht angebotener SERVICE-Code: " + serviceCode));
                valid = false;
            }

            if (valid) {
                appointments.add(new ValidatedImportAppointment(lineNumber, customerNumber, start,
                        offer.serviceId(), offer.durationMinutes(), serviceCode));
            }
        }

        if (lines.length <= 1 || appointments.isEmpty() && errors.isEmpty()) {
            errors.add(new AppointmentImportError(2, "CSV enthält keine Termindatensätze"));
        }
        if (!errors.isEmpty()) {
            throw new AppointmentImportValidationException(errors);
        }
        return List.copyOf(appointments);
    }

    private String decodeUtf8(byte[] csv) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(csv)).toString();
        } catch (CharacterCodingException exception) {
            throw new AppointmentImportValidationException(
                    List.of(new AppointmentImportError(1, "Datei ist nicht gültig UTF-8-kodiert")));
        }
    }

    private String stripBom(String value) {
        return value.startsWith("\uFEFF") ? value.substring(1) : value;
    }

    private List<String> parseRecord(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        boolean quoteClosed = false;
        for (int index = 0; index < line.length(); index++) {
            char current = line.charAt(index);
            if (quoted) {
                if (current == '"') {
                    if (index + 1 < line.length() && line.charAt(index + 1) == '"') {
                        field.append('"');
                        index++;
                    } else {
                        quoted = false;
                        quoteClosed = true;
                    }
                } else {
                    field.append(current);
                }
            } else if (current == ',') {
                fields.add(field.toString());
                field.setLength(0);
                quoteClosed = false;
            } else if (current == '"') {
                if (field.length() != 0 || quoteClosed) {
                    throw new IllegalArgumentException("Anführungszeichen an ungültiger Position");
                }
                quoted = true;
            } else if (quoteClosed && !Character.isWhitespace(current)) {
                throw new IllegalArgumentException("Unerwarteter Inhalt nach einem Anführungszeichen");
            } else if (!quoteClosed) {
                field.append(current);
            }
        }
        if (quoted) {
            throw new IllegalArgumentException("Anführungszeichen nicht geschlossen");
        }
        fields.add(field.toString());
        return fields;
    }
}
