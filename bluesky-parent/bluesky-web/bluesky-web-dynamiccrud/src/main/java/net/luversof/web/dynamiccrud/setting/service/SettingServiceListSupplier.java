package net.luversof.web.dynamiccrud.setting.service;

import java.util.List;

import net.luversof.web.dynamiccrud.setting.domain.Setting;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;

@FunctionalInterface
public interface SettingServiceListSupplier<T extends Setting> extends SettingService<T> {

	List<T> findList(SettingParameter settingParameter);

}
