package net.luversof.opensource.mybatis.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.type.TypeHandler;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import net.luversof.opensource.mybatis.type.LocalDateTimeTypeHandler;

public abstract class MybatisConfig {
	public SqlSessionFactoryBean getSqlSessionFactoryBean() throws Exception {

		SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
		sqlSessionFactoryBean.setMapperLocations(
				new PathMatchingResourcePatternResolver().getResources("classpath*:**/*Mapper.xml"));
		List<TypeHandler<?>> typeHandlerlist = new ArrayList<>();
		typeHandlerlist.add(new LocalDateTimeTypeHandler());
		TypeHandler<?>[] typeHandlers = typeHandlerlist.toArray(new	TypeHandler[typeHandlerlist.size()]);
		sqlSessionFactoryBean.setTypeHandlers(typeHandlers);
		sqlSessionFactoryBean.setTypeHandlersPackage("net.luversof.opensource.mybatis.type");
		return sqlSessionFactoryBean;
	}
}
