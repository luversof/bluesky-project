package net.luversof.web.gate.vaadin;

import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

/**
 * 공통 규칙을 지정하면 좋지 않을까?
 * 근데 이런식으로 규격 정하는게 좋은 방법일까?
 * interface는 public 규칙을 정하는 건데...
 * abstract 로 해야 맞지 않을까? -> extends vaadin component class가 있어서 안되네..
 * 
 */
public interface GateVaadin extends BeforeEnterObserver, LocaleChangeObserver {
	
	@Override
	default void beforeEnter(BeforeEnterEvent event) {
		setInstanceVariable(event);
		createView();
	}
	
	@Override
	default void localeChange(LocaleChangeEvent event) {
		updateLocale();
	}
	
	/**
	 * instance variable 설정
	 * 변수 관리를 좀더 고도화 한다면 이 로직을 변경할 지도 모르겠음 
	 */
	default void setInstanceVariable(BeforeEnterEvent event) {
		
	}

	/**
	 * 다국어 설정을 모아두는 곳
	 * 언어 변경시 반영을 위해 제공
	 */
	default void updateLocale() {
		
	}
	
	/**
	 * 실제 UI 설정하는 곳
	 * 한번만 호출되어야 함.
	 */
	default void createView() {
		
	}

}
