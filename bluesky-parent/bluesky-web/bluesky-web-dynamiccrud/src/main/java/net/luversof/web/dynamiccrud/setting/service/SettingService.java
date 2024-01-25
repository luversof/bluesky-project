package net.luversof.web.dynamiccrud.setting.service;

import net.luversof.web.dynamiccrud.setting.domain.Setting;

public interface SettingService<T extends Setting> {

	/**
	 * 각 SettingService간 순서 지정을 위해 제공
	 * 
	 * @return
	 */
	default int getOrder() {
		return 0;
	};
}
