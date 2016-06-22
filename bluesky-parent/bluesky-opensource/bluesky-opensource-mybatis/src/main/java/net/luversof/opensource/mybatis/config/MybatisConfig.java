package net.luversof.opensource.mybatis.config;

import org.apache.ibatis.type.MappedTypes;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.boot.autoconfigure.SpringBootVFS;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public abstract class MybatisConfig {
	public SqlSessionFactoryBean getSqlSessionFactoryBean() throws Exception {

		SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
		sqlSessionFactoryBean.setMapperLocations(
				new PathMatchingResourcePatternResolver().getResources("classpath*:**/*Mapper.xml"));
		sqlSessionFactoryBean.setVfs(SpringBootVFS.class);
//		List<TypeHandler<?>> typeHandlerlist = new ArrayList<>();
//		typeHandlerlist.add(new LocalDateTimeTypeHandler());
//		TypeHandler<?>[] typeHandlers = typeHandlerlist.toArray(new	TypeHandler[typeHandlerlist.size()]);
//		sqlSessionFactoryBean.setTypeHandlers(typeHandlers);
		sqlSessionFactoryBean.setTypeHandlersPackage("net.luversof.opensource.mybatis.type");
//		sqlSessionFactoryBean.setTypeHandlersPackage("org.apache.ibatis.type");	// 이거 @MappedTypes 가 선언안되어 있네?
		return sqlSessionFactoryBean;
	}
}
