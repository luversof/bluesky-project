package net.luversof.data.jpa.service;

import java.io.Serializable;



import net.luversof.jdbc.datasource.DataSource;

import org.springframework.data.repository.CrudRepository;

@DataSource
public abstract class GeneralService<T, ID extends Serializable>  {
	public abstract <S extends CrudRepository<T, ID>> S getRepository();
	
	public <S extends T> S save(S entity) {
		return getRepository().save(entity);
	};
	
	public T findOne(ID id) {
		return getRepository().findOne(id);
	}
}
