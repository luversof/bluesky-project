package net.luversof.web.gate.vaadin;

import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.BeforeEnterObserver;

/**
 * 공통 규칙을 지정하면 좋지 않을까?
 */
public interface GateVaadin extends BeforeEnterObserver, LocaleChangeObserver {
	
	@Override
	default void localeChange(LocaleChangeEvent event) {
		updateLocale();
	}

	default void updateLocale() {
		
	}

}
