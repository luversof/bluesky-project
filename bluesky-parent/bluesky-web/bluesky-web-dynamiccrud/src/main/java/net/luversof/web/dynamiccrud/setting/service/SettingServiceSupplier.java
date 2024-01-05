package net.luversof.web.dynamiccrud.setting.service;

import net.luversof.web.dynamiccrud.setting.domain.Setting;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;

@FunctionalInterface
public interface SettingServiceSupplier<T extends Setting> extends SettingService<T> {

	T findOne(SettingParameter settingParameter);

}
