package net.luversof.api.bookkeeping.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** 
 * 반복 코드를 줄이려고 만들어봄
 * @param <T>
 */
public interface BasicCrudService<T, ID> {
	
	JpaRepository<T, ID> getRepository();

	default T save(T t) {
		return getRepository().save(t);
	}
	
	default List<T> saveAll(Iterable<T> list) {
		return getRepository().saveAll(list);
	}
	
	default Optional<T> findById(ID id) {
		return getRepository().findById(id);
	}

	default T update(T t) {
		return getRepository().save(t);
	}
	
	default void delete(T t) {
		getRepository().delete(t);
	}

}
