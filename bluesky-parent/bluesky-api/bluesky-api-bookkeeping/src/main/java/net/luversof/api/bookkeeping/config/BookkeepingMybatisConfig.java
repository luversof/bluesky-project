package net.luversof.api.bookkeeping.config;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import io.github.luversof.boot.autoconfigure.mybatis.type.UUIDTypeHandler;


@Configuration
@MapperScan(basePackages = "net.luversof.api.bookkeeping.mapper", sqlSessionFactoryRef = "bookkeepingSqlSessionFactory")
public class BookkeepingMybatisConfig {
    @Bean
    SqlSessionFactory bookkeepingSqlSessionFactory(@Qualifier("bookkeepingDataSource") DataSource bookkeepingDataSource) throws Exception {
        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
        sqlSessionFactoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:net/luversof/api/bookkeeping/mapper/xml/*Mapper.xml")
        );
        sqlSessionFactoryBean.setTypeHandlersPackage("net.luversof.boot.autoconfigure.mybatis.type");
        sqlSessionFactoryBean.setTypeHandlers(new UUIDTypeHandler());
        sqlSessionFactoryBean.setDataSource(bookkeepingDataSource);
        sqlSessionFactoryBean.afterPropertiesSet();
        return sqlSessionFactoryBean.getObject();
    }
}
