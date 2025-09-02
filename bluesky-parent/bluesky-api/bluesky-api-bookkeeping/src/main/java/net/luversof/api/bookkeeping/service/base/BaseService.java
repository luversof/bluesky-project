package net.luversof.api.bookkeeping.service.base;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

/** 
 * 개발시 데이터 확인을 위해 제공하는 기초적인 호출을 제공
 * @param <T>
 */
public interface BaseService<T, ID> {
	
	CrudRepository<T, ID> getRepository();

	default T save(T t) {
		return getRepository().save(t);
	}
	
	default Iterable<T> saveAll(Iterable<T> list) {
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
	
	default void deleteById(ID id) {
		getRepository().deleteById(id);
	}

}
