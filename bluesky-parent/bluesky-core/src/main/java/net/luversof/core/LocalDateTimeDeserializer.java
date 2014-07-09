package net.luversof.core;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * LocalDateTime 형태로 저장하면서 LocalDate 형태로 표현하려고 만들었음.
 * 포맷을 맞추는게 맞는 방향.
 * 현재 사용하지 않음
 * @author choiyong-rak
 *
 */
public class LocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

	@Override
	public LocalDateTime deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		String[] dateParts = jp.getValueAsString().split("-");
		return LocalDateTime.of(Integer.valueOf(dateParts[0]), Month.of(Integer.valueOf(dateParts[1])), Integer.valueOf(dateParts[2]), 0, 0);
	}

}
