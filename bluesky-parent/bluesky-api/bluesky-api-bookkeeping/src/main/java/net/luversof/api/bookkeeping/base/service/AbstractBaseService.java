package net.luversof.api.bookkeeping.base.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public abstract class AbstractBaseService<T, ID> implements BaseService<T, ID> {
	
	public abstract JpaRepository<T, ID> getRepository(); 
	
	@Override
	public T save(T t) {
		return getRepository().save(t);
	}
	
	@Override
	public List<T> saveAll(Iterable<T> t) {
		return getRepository().saveAll(t);
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
