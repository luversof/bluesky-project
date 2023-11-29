package net.luversof.web.dynamiccrud.setting.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;

import net.luversof.web.dynamiccrud.setting.domain.Setting;

@NoRepositoryBean
public interface SettingRepository<T extends Setting, ID> extends CrudRepository<T, ID> {

	<S extends T> Page<S> findAll(Pageable pageable);

}
