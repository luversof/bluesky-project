package net.luversof.app.google.stock.databind;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class StockDateDeserializer extends ValueDeserializer<Instant> {

    private static final List<DateTimeFormatter> DATE_FORMATTERS =
            List.of(
                    DateTimeFormatter.ofPattern("yyyy. M. d"),
                    DateTimeFormatter.ofPattern("yyyy-M-d"),
                    DateTimeFormatter.ISO_LOCAL_DATE);

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
        String value = p.getString();
        if (value == null || value.isBlank()) return null;

        String trimmed = value.trim();

        for (var formatter : DATE_FORMATTERS) {
            try {
                LocalDate localDate = LocalDate.parse(trimmed, formatter);
                // 한국 시간 기준 15:00 저장 (UTC +9) -> 00:00 저장 이슈 (전일 15:00)
                // 한국 시장 개장 시간인 09:00 기준으로 저장하여 UTC 00:00 으로 맞춤
                return localDate.atTime(9, 0).atZone(KST).toInstant();
            } catch (DateTimeParseException e) {
                // ignore
            }
        }

        return null;
    }
}
