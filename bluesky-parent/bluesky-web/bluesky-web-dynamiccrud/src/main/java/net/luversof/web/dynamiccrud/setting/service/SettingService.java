package net.luversof.web.dynamiccrud.setting.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import net.luversof.web.dynamiccrud.setting.domain.AbstractSettingClass;
import net.luversof.web.dynamiccrud.setting.repository.SettingRepository;

public class SettingService<T extends AbstractSettingClass> {

	@Autowired
	private SettingRepository<T> settingRepository;
	
	public Page<T> findAll(Pageable pabeable) {
		return settingRepository.findAll(pabeable);
	}
}
