package net.luversof.web.dynamiccrud.use.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.MultiValueMap;

import net.luversof.web.dynamiccrud.setting.domain.Field;
import net.luversof.web.dynamiccrud.setting.domain.Query;

public interface UseService {
	
	Page<Map<String, Object>> find(Query query, List<Field> fieldList, Pageable pageable, Map<String, String> dataMap);
	
	Object create(Query query, List<Field> fieldList, Map<String, String> dataMap);
	
	Object update(Query query, List<Field> fieldList, Map<String, String> dataMap);
	
	Object delete(Query query, List<Field> fieldList, MultiValueMap<String, String> dataMap);

}
