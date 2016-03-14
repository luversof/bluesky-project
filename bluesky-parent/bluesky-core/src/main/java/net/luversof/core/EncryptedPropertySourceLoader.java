package net.luversof.core;

import java.io.IOException;
import java.util.Properties;

import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.spring31.properties.EncryptablePropertiesPropertySource;
import org.springframework.boot.env.PropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import net.luversof.core.util.EncryptionUtil;

@Deprecated
public class EncryptedPropertySourceLoader implements PropertySourceLoader {
	
	private final StandardPBEStringEncryptor encryptor = (StandardPBEStringEncryptor) EncryptionUtil.stringEncryptor();

	public EncryptedPropertySourceLoader() {
		// TODO: this could be taken from an environment variable
		// this.encryptor.setPassword("password");
	}

	@Override
	public String[] getFileExtensions() {
		return new String[] { "properties" };
	}

	@Override
	public PropertySource<?> load(final String name, final Resource resource, final String profile) throws IOException {
		if (profile == null) {
			final Properties props = PropertiesLoaderUtils.loadProperties(resource);

			if (!props.isEmpty()) {
				return new EncryptablePropertiesPropertySource(name, props, this.encryptor);
			}
		}

		return null;
	}
}