package net.luversof.web.dynamiccrud.setting.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import net.luversof.web.dynamiccrud.setting.domain.Setting;

public interface SettingServiceDecorator {

	/**
	 * Decorator에서 여러 settingServiceMap을 호출하여 사용할 때 순서를 지정하기 위해 제공
	 * 
	 * @param <T>
	 * @param <S>
	 * @param settingServiceMap
	 * @return
	 */
	default <T extends Setting, S extends SettingService<T>> Map<String, S> getSortedSettingServiceMap(Map<String, S> settingServiceMap) {
		return settingServiceMap
				.entrySet()
				.stream()
				.sorted(Comparator.comparing(e -> e.getValue().getOrder()))
				.collect(Collectors.toMap(Entry::getKey, Entry::getValue, (x, y) -> y, LinkedHashMap::new));
	}
}
