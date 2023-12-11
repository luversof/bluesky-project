package net.luversof.web.dynamiccrud.use.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import net.luversof.web.dynamiccrud.setting.domain.Field;
import net.luversof.web.dynamiccrud.setting.domain.Query;

public interface UseService {
	
	Page<Map<String, Object>> find(Query query, List<Field> fieldList, Pageable pageable, Map<String, String> paramMap);
	
	Object insert(Query query, List<Field> fieldList, Map<String, String> postData);

}
