package net.luversof.core;

import org.jasypt.properties.PropertyValueEncryptionUtils;
import org.springframework.core.convert.converter.Converter;
 
public class EncryptStringConverter implements Converter<String, String> {

	@Override
	public String convert(String source) {
		if (!PropertyValueEncryptionUtils.isEncryptedValue(source)) {
			return source;
		}
		return PropertyValueEncryptionUtils.decrypt(source, EncryptionUtil.stringEncryptor());
	}

}
