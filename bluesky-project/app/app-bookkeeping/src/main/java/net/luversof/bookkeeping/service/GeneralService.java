package net.luversof.bookkeeping.service;

import java.io.Serializable;

import net.luversof.data.jpa.datasource.DataSource;

import org.springframework.data.repository.CrudRepository;

@DataSource
public abstract class GeneralService<T, ID extends Serializable>  {
	abstract <S extends CrudRepository<T, ID>> S getRepository();
	
	public <S extends T> S save(S entity) {
		return getRepository().save(entity);
	};
}
