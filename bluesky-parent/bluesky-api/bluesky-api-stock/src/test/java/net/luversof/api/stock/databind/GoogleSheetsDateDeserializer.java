package net.luversof.api.stock.databind;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class GoogleSheetsDateDeserializer extends JsonDeserializer<Instant> {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy. M. d");

	@Override
	public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		String value = p.getText();
		if (value == null || value.isBlank())
			return null;

		try {
			LocalDate localDate = LocalDate.parse(value.trim(), FORMATTER);
			return localDate.atStartOfDay().toInstant(ZoneOffset.ofHours(9));
		} catch (Exception e) {
			return null;
		}
	}
}
