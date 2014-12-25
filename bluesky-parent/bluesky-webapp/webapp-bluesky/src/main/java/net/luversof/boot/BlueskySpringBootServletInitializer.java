package net.luversof.boot;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import net.luversof.security.SecurityConfig;
import net.luversof.web.BlueskyWebConfig;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

@SpringBootApplication
public class BlueskySpringBootServletInitializer extends SpringBootServletInitializer {


//	@Override
//	protected WebApplicationContext createRootApplicationContext(ServletContext servletContext) {
//		AnnotationConfigWebApplicationContext rootAppContext = new AnnotationConfigWebApplicationContext();
//		rootAppContext.register(new Class[] { BlogConfig.class, BookkeepingConfig.class, SecurityConfig.class });
//		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, rootAppContext);
//		// TODO Auto-generated method stub
//		return super.createRootApplicationContext(servletContext);
//	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		// TODO Auto-generated method stub
//		application.parent();
//		application.sources(BlueskyWebConfig.class, BlogConfig.class, BookkeepingConfig.class, SecurityConfig.class);
		return super.configure(application);
	}
	

}
