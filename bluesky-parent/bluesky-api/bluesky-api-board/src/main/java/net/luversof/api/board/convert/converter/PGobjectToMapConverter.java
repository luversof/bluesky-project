package net.luversof.api.board.convert.converter;

import java.util.Map;

import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * PostgreSQL JSONB 컬럼 처리를 위한 PGobject to Map Converter
 */
@ReadingConverter
public class PGobjectToMapConverter implements Converter<PGobject, Map<String, Object>> {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public Map<String, Object> convert(PGobject source) {
		if (source == null || source.getValue() == null) {
			return null;
		}
		try {
			return objectMapper.readValue(source.getValue(), new TypeReference<Map<String, Object>>() {});
		} catch (Exception e) {
			throw new IllegalArgumentException("Error converting PGobject JSONB to Map", e);
		}
	}
}