package net.luversof.api.board.convert.converter;

import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@WritingConverter
public class MapToStringConverter implements Converter<Map<String, Object>, String> {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public String convert(Map<String, Object> source) {
		try {
			return objectMapper.writeValueAsString(source);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("Error converting Map to JSON", e);
		}
	}
}
