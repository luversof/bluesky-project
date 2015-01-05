package net.luversof.opensource.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import lombok.extern.slf4j.Slf4j;
import net.luversof.opensource.jdbc.routing.DataSourceContextHolder;
import net.luversof.opensource.jdbc.routing.DataSourceType;
import net.luversof.opensource.jdbc.routing.RoutingDataSource;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;


@Slf4j
public class JdbcTest extends GeneralTest {
	
	@Autowired
	private RoutingDataSource routingDataSource;

	@Test
	public void test() {
		DataSourceContextHolder.setDataSourceType(DataSourceType.BOOKKEEPING);
		DataSourceContextHolder.setDataSourceType(DataSourceType.BLOG);
		Connection connection;
		try {
			connection = routingDataSource.getConnection();
			PreparedStatement pstmt = connection.prepareStatement("SELECT * FROM Blog");
			ResultSet resultSet = pstmt.executeQuery();
			log.debug("resultSet : {}", resultSet);
			resultSet.next();
			pstmt.close();
			connection.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Value("${datasource.blog.username}")
	private String a;

	@Test
	public void test2() {
		System.out.println(a);
	}
}
