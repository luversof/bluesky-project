package net.luversof.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import lombok.extern.slf4j.Slf4j;
import net.luversof.jdbc.datasource.DataSourceContextHolder;
import net.luversof.jdbc.datasource.DataSourceType;
import net.luversof.jdbc.datasource.RoutingDataSource;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;


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
}
