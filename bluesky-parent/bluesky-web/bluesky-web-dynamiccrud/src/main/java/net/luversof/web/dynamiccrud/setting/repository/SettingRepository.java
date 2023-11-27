package net.luversof.web.dynamiccrud.setting.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;

import net.luversof.web.dynamiccrud.setting.domain.AbstractSettingClass;

public interface SettingRepository<T extends AbstractSettingClass> extends CrudRepository<T, Long> {

	<S extends T> Page<S> findAll(Pageable pageable);
	<S extends T> Page<S> findByName(S s, Pageable pageable);

}
