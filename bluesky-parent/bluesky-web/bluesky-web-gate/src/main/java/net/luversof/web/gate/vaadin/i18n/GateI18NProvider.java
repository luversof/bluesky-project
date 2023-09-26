package net.luversof.web.gate.vaadin.i18n;

import java.util.List;
import java.util.Locale;

import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.stereotype.Component;

import com.vaadin.flow.i18n.I18NProvider;

@Component
public class GateI18NProvider implements I18NProvider {
	
	private static final long serialVersionUID = 1L;
	
	private MessageSourceAccessor messageSourceAccessor;
	
	public GateI18NProvider(MessageSourceAccessor messageSourceAccessor) {
		this.messageSourceAccessor = messageSourceAccessor;
	}

	@Override
	public List<Locale> getProvidedLocales() {
		return List.of(Locale.KOREA, Locale.US);
	}

	@Override
	public String getTranslation(String key, Locale locale, Object... params) {
		return messageSourceAccessor.getMessage(key, params, locale);
	}

}
