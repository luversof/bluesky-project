package net.luversof.api.bookkeeping.controller.base;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.luversof.api.bookkeeping.service.base.BaseService;

@RestController
public interface BaseController<T, ID> {
	
	BaseService<T, ID> getService();
	
	ObjectMapper getObjectMapper();
	
	@PostMapping
	default ResponseEntity<?> save(@RequestBody JsonNode requestBody) {
		if (requestBody.isArray()) {
			List<T> list = new ArrayList<>();
			requestBody.forEach(node -> list.add(getObjectMapper().convertValue(node, new TypeReference<T>() {})));
			return ResponseEntity.status(HttpStatus.CREATED).body(getService().saveAll(list));
		}
		
		T t = getObjectMapper().convertValue(requestBody, new TypeReference<T>() {});
		return ResponseEntity.status(HttpStatus.CREATED).body(getService().save(t));
	}
	
	@GetMapping("/{id}")
	default Optional<T> findById(@PathVariable ID id) {
		return getService().findById(id);
	}

	@PutMapping("/{id}")
	default T update(@RequestBody T t) {
		return getService().save(t);
	}
	
	@DeleteMapping
	default void delete(T t) {
		getService().delete(t);
	}

}
