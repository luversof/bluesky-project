package net.luversof.api.bookkeeping.service;

import java.util.Optional;

/** 
 * 기본 처리 서비스를 작성하는 비용을 줄이기 위해 만듬
 * @param <T>
 */
public interface CommonService<T, ID> {

	T create(T t);
	
	Optional<T> findById(ID id);

	T update(T t);
	
	void delete(T t);

}
