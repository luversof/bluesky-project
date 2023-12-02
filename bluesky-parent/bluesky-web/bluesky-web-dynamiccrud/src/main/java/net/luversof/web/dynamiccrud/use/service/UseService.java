package net.luversof.web.dynamiccrud.use.service;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import net.luversof.web.dynamiccrud.setting.domain.Query;

public interface UseService {
	
	Page<Map<String, Object>> find(Query query, Pageable pageable);

}
