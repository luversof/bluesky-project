package net.luversof.web.dynamiccrud.use.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.MultiValueMap;

import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.domain.DbQuery;

public interface UseService {
	
	Page<Map<String, Object>> find(DbQuery query, List<DbField> fieldList, Pageable pageable, Map<String, String> dataMap);
	
	Object create(DbQuery query, List<DbField> fieldList, Map<String, String> dataMap);
	
	Object update(DbQuery query, List<DbField> fieldList, Map<String, String> dataMap);
	
	Object delete(DbQuery query, List<DbField> fieldList, MultiValueMap<String, String> dataMap);

}
