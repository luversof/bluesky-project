package net.luversof.api.bookkeeping.service;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public abstract class AbstractCommonService<T, ID> implements CommonService<T, ID> {
	
	public abstract JpaRepository<T, ID> getRepository(); 
	
	@Override
	public T create(T t) {
		return getRepository().save(t);
	}

	@Override
	public Optional<T> findById(ID id) {
		return getRepository().findById(id);
	}

	@Override
	public T update(T t) {
		return getRepository().save(t);
	}

	@Override
	public void delete(T t) {
		getRepository().delete(t);
	}
	
	
}
