package net.luversof.web.gate.thymeleaf.crudtest.resolver;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CrudResolver<T> {

	Page<T> selectAllList(Pageable pageable);
	
	T select(T t);
	
	T create(T t);
	
	T update(T t);
	
	T delete(T t);
}
