package net.luversof.api.board.convert.util;

import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.util.ReflectionUtils;

import io.github.luversof.boot.uuid.UuidGeneratorUtil;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DataJdbcConverterUtil {
	
	public static <T> T prepareEntity(T entity) {
		setId(entity);
		return entity;
	}

	public static <T> boolean isNewEntity(T entity) {
		for (var field : entity.getClass().getDeclaredFields()) {
			try {
				if (field.isAnnotationPresent(Id.class)) {
					ReflectionUtils.makeAccessible(field);
					Object value = field.get(entity);
					return (value == null
						|| (value instanceof String string && string.isEmpty())
						|| (value instanceof Number number && number.longValue() == 0)
					);
				}
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				return true;
			}
		}
		return true;
	}
	
	public static <T> void setId(T entity) {
		if (!isNewEntity(entity)) {
			return;
		}
		
		for (var field : entity.getClass().getDeclaredFields()) {
			if (field.isAnnotationPresent(Id.class) && field.getType().equals(UUID.class)) {
				ReflectionUtils.makeAccessible(field);
				ReflectionUtils.setField(field, entity, UuidGeneratorUtil.getUuid());
			}
		}
	}
}
