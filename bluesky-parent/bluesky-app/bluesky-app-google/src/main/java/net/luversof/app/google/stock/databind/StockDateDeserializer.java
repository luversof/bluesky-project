package net.luversof.app.google.stock.databind;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class StockDateDeserializer extends ValueDeserializer<Instant> {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy. M. d");

	@Override
	public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
		String value = p.getString();
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
