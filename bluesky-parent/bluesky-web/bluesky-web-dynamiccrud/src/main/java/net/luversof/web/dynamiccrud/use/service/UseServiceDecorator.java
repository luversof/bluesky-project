package net.luversof.web.dynamiccrud.use.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud.setting.domain.Field;
import net.luversof.web.dynamiccrud.setting.domain.Query;
import net.luversof.web.dynamiccrud.setting.domain.QueryDbType;

@Service
public class UseServiceDecorator implements UseService {
	
	@Autowired
	private Map<String, UseService> useServiceMap;

	@Override
	public Page<Map<String, Object>> find(Query query, List<Field> fieldList, Pageable pageable, Map<String, String> dataMap) {
		if (query.getDbType().equals(QueryDbType.MySql)) {
			UseService useService = useServiceMap.get("mariadbUseService");
			
			// query 기준으로 결과 반환 해야 하는데..
			return useService.find(query, fieldList, pageable, dataMap);
		}
		return null;
	}
	
	@Override
	public Object create(Query query, List<Field> fieldList, Map<String, String> dataMap) {
		if (query.getDbType().equals(QueryDbType.MySql)) {
			UseService useService = useServiceMap.get("mariadbUseService");
			
			return useService.create(query, fieldList, dataMap);
		}
		return null;
	}

	@Override
	public Object update(Query query, List<Field> fieldList, Map<String, String> dataMap) {
		if (query.getDbType().equals(QueryDbType.MySql)) {
			UseService useService = useServiceMap.get("mariadbUseService");
			
			return useService.update(query, fieldList, dataMap);
		}
		return null;
	}

	@Override
	public Object delete(Query query, List<Field> fieldList, Map<String, String> dataMap) {
		if (query.getDbType().equals(QueryDbType.MySql)) {
			UseService useService = useServiceMap.get("mariadbUseService");
			
			return useService.delete(query, fieldList, dataMap);
		}
		return null;
	}
}
