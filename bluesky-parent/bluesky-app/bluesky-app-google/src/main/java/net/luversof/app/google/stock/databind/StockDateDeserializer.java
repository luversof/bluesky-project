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

	private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
			DateTimeFormatter.ofPattern("yyyy. M. d"),
			DateTimeFormatter.ofPattern("yyyy-M-d"),
			DateTimeFormatter.ISO_LOCAL_DATE);
	
	private static final ZoneId KST = ZoneId.of("Asia/Seoul");

	@Override
	public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
		String value = p.getString();
		if (value == null || value.isBlank())
			return null;
		
		String trimmed = value.trim();

		for (var formatter : DATE_FORMATTERS) {
			try {
				LocalDate localDate = LocalDate.parse(trimmed, formatter);
				return localDate.atStartOfDay(KST).toInstant();
			} catch (DateTimeParseException e) {
				// ignore
			}
		}
		
		return null;
	}
}
