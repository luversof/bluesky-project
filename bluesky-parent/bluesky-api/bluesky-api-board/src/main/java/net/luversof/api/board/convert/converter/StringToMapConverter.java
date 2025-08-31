package net.luversof.api.board.convert.converter;

import java.io.IOException;
import java.util.Map;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@ReadingConverter
public class StringToMapConverter implements Converter<String, Map<String, Object>> {
	
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public Map<String, Object> convert(String source) {
		try {
			return objectMapper.readValue(source, new TypeReference<Map<String, Object>>() {});
		} catch (IOException e) {
			throw new IllegalArgumentException("Error converting JSON to Map", e);
		}
	}
}