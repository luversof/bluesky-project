package net.luversof.web.dynamiccrud.use.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.domain.DbQuery;
import net.luversof.web.dynamiccrud.setting.domain.DbQueryDbType;

@Service
public class UseServiceDecorator implements UseService {
	
	@Autowired
	private Map<String, UseService> useServiceMap;

	@Override
	public Page<Map<String, Object>> find(DbQuery query, List<DbField> fieldList, Pageable pageable, Map<String, String> dataMap) {
		if (query.getDbType().equals(DbQueryDbType.MySql)) {
			UseService useService = useServiceMap.get("mariadbUseService");
			
			// query 기준으로 결과 반환 해야 하는데..
			return useService.find(query, fieldList, pageable, dataMap);
		}
		return null;
	}
	
	@Override
	public Object create(DbQuery query, List<DbField> fieldList, Map<String, String> dataMap) {
		if (query.getDbType().equals(DbQueryDbType.MySql)) {
			UseService useService = useServiceMap.get("mariadbUseService");
			
			return useService.create(query, fieldList, dataMap);
		}
		return null;
	}

	@Override
	public Object update(DbQuery query, List<DbField> fieldList, Map<String, String> dataMap) {
		if (query.getDbType().equals(DbQueryDbType.MySql)) {
			UseService useService = useServiceMap.get("mariadbUseService");
			
			return useService.update(query, fieldList, dataMap);
		}
		return null;
	}

	@Override
	public Object delete(DbQuery query, List<DbField> fieldList, MultiValueMap<String, String> dataMap) {
		if (query.getDbType().equals(DbQueryDbType.MySql)) {
			UseService useService = useServiceMap.get("mariadbUseService");
			
			return useService.delete(query, fieldList, dataMap);
		}
		return null;
	}
}
