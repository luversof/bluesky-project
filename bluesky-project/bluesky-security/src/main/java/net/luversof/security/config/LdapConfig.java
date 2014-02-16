package net.luversof.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.security.config.annotation.authentication.configurers.ldap.LdapAuthenticationProviderConfigurer.ContextSourceBuilder;
import org.springframework.security.ldap.authentication.BindAuthenticator;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.search.FilterBasedLdapUserSearch;
import org.springframework.security.ldap.search.LdapUserSearch;

@Configuration
public class LdapConfig {

	@Bean
	public LdapAuthenticationProvider ldapAuthenticationProvider() {
		LdapAuthenticationProvider ldapAuthenticationProvider = new LdapAuthenticationProvider(authenticator());
		return ldapAuthenticationProvider;
	}
	
	@Bean
	public LdapAuthenticator authenticator() {
		BindAuthenticator authenticator = new BindAuthenticator(contextSource());
		//authenticator.setUserSearch(ldapUserSearch());
		authenticator.setUserDnPatterns(new String[]{"uid={0},ou=people"});
		return authenticator;
	}
	
	@Bean
	public LdapContextSource contextSource() {
		LdapContextSource ldapContextSource = new LdapContextSource();
		ldapContextSource.setUrl("ldap://127.0.0.1:33389");
		ldapContextSource.setBase("dc=springframework,dc=org");
		ldapContextSource.setUserDn("uid={0},ou=people");
		return ldapContextSource;
	}
	
	@Bean
	public LdapUserSearch ldapUserSearch () {
		LdapUserSearch ldapUserSearch = new FilterBasedLdapUserSearch("", "(&((uid={0})(ou=people)))", contextSource());
		return ldapUserSearch;
	}
}

