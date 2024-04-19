package net.luversof.api.bookkeeping.base.service;

import java.util.List;
import java.util.Optional;

/** 
 * 기본 처리 서비스를 작성하는 비용을 줄이기 위해 만듬
 * 이거 필요없을거 같은데? 아닌가...
 * @param <T>
 */
public interface BaseService<T, ID> {

	T save(T t);
	
	List<T> saveAll(Iterable<T> list);
	
	Optional<T> findById(ID id);

	T update(T t);
	
	void delete(T t);

}
