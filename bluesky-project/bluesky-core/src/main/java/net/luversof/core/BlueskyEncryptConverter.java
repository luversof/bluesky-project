package net.luversof.core;

import org.jasypt.encryption.StringEncryptor;
import org.jasypt.properties.PropertyValueEncryptionUtils;
import org.springframework.core.convert.converter.Converter;
 
public class BlueskyEncryptConverter implements Converter<String, String> {

	final static StringEncryptor STRING_ENCRYPTOR = BlueskyEncryptUtil.stringEncryptor();
	@Override
	public String convert(String source) {
		if (!PropertyValueEncryptionUtils.isEncryptedValue(source)) {
			return source;
		}
		return PropertyValueEncryptionUtils.decrypt(source, STRING_ENCRYPTOR);
	}

}
