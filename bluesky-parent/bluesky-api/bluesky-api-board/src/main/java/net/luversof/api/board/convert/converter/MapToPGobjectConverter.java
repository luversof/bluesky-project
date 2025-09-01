package net.luversof.api.board.convert.converter;

import java.sql.SQLException;
import java.util.Map;

import org.postgresql.util.PGobject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * PostgreSQL JSONB 컬럼 처리를 위한 Map to PGobject Converter
 */
@WritingConverter
public class MapToPGobjectConverter implements Converter<Map<String, Object>, PGobject> {

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Override
	public PGobject convert(Map<String, Object> source) {
		try {
			PGobject jsonObject = new PGobject();
			jsonObject.setType("jsonb"); // PostgreSQL jsonb 타입 명시
			jsonObject.setValue(objectMapper.writeValueAsString(source));
			return jsonObject;
		} catch (JsonProcessingException | SQLException e) {
			throw new IllegalArgumentException("Error converting Map to JSONB", e);
		}
	}
}