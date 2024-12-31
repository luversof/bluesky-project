package net.luversof.api.bookkeeping.controller.base;

import java.util.List;
import java.util.Optional;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.bookkeeping.service.base.BaseService;

@RestController
public interface BaseController<T, ID> {
	
	BaseService<T, ID> getService();
	
	@PostMapping
	default T save(@RequestBody T t) {
		return getService().save(t);
	}
	
	@PostMapping("/saveAll")
	default List<T> saveAll(@RequestBody Iterable<T> list) {
		return getService().saveAll(list);
	}
	
	@GetMapping("/{id}")
	default Optional<T> findById(ID id) {
		return getService().findById(id);
	}

	@PutMapping
	default T update(@RequestBody T t) {
		return getService().save(t);
	}
	
	@DeleteMapping
	default void delete(T t) {
		getService().delete(t);
	}

}
