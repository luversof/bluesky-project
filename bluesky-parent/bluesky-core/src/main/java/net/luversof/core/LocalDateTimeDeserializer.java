package net.luversof.core;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

	@Override
	public LocalDateTime deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		String[] dateParts = jp.getValueAsString().split("-");
		return LocalDateTime.of(Integer.valueOf(dateParts[0]), Month.of(Integer.valueOf(dateParts[1])), Integer.valueOf(dateParts[2]), 0, 0);
	}

}
