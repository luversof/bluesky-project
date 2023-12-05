package net.luversof.web.dynamiccrud.use.util;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;

import lombok.experimental.UtilityClass;
import net.luversof.web.dynamiccrud.setting.domain.Field;

@UtilityClass
public class ThymeleafUseUtil {

	
	public LinkedHashMap<String, String> getColumnMap(Page<Map<String, Object>> page, List<Field> fieldList) {
		if (page == null || !page.hasContent()) {
			return null;
		}
		
		return getColumnMap(page.getContent().get(0),  fieldList);
	}
	
	

	public LinkedHashMap<String, String> getColumnMap(Map<String, Object> data, List<Field> fieldList) {
		if (data == null || data.isEmpty()) {
			return null;
		}
		
		var map = new LinkedHashMap<String, String>();
		
		// Field 정보가 있으면 설정된 field에 대해서만 노출 처리
		if (fieldList != null && !fieldList.isEmpty()) {
			var targetFieldList = fieldList.stream().filter(Field::isVisible).sorted(Comparator.comparing(Field::getFormOrder)).collect(Collectors.toList());
			targetFieldList.forEach(field -> map.put(field.getColumn(), field.getName()));
		} else {
			// Field 정보가 없으면 그냥 전체 노출 처리
			data.keySet().forEach(key -> {
				if (!map.containsKey(key)) {
					map.put(key, key);
				}
			});
		}
		
		
		return map;
	}
}
