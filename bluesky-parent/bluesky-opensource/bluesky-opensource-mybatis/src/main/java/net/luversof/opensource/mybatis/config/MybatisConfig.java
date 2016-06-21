package net.luversof.opensource.mybatis.config;

import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.boot.autoconfigure.SpringBootVFS;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

public abstract class MybatisConfig {
	public SqlSessionFactoryBean getSqlSessionFactoryBean() throws Exception {

		SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
		sqlSessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:**/*Mapper.xml"));
		sqlSessionFactoryBean.setVfs(SpringBootVFS.class);
		sqlSessionFactoryBean.setTypeHandlersPackage("net.luversof.opensource.mybatis.type");
		return sqlSessionFactoryBean;
	}
}
