package net.luversof.sql.analyzer.opensource.mybatis.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.type.TypeHandler;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import net.luversof.boot.autoconfigure.mybatis.type.UUIDTypeHandler;


@Configuration
@MapperScan(basePackages = "net.luversof.sql.analyzer.mapper", sqlSessionFactoryRef = "sqlAnalyzerSqlSessionFactory")
public class MybatisSqlAnalyzerConfig {
	@Bean
	public SqlSessionFactory sqlAnalyzerSqlSessionFactory(@Qualifier("sqlAnalyzerDataSource") DataSource sqlAnalyzerDataSource) throws Exception {
		SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
		sqlSessionFactoryBean.setMapperLocations(
				new PathMatchingResourcePatternResolver().getResources("classpath*:net/luversof/sql/analyzer/mapper/xml/*Mapper.xml")
			);
		sqlSessionFactoryBean.setTypeHandlersPackage("net.luversof.boot.autoconfigure.mybatis.type");
		sqlSessionFactoryBean.setTypeHandlers(new TypeHandler[]{new UUIDTypeHandler()});
		sqlSessionFactoryBean.setDataSource(sqlAnalyzerDataSource);
		sqlSessionFactoryBean.afterPropertiesSet();
		return sqlSessionFactoryBean.getObject();
	}
}
