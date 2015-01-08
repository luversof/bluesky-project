package net.luversof.opensource.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
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
			printResultSet(resultSet);
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
	
	private void printResultSet(ResultSet resultSet) throws SQLException {
		ResultSetMetaData rsmd = resultSet.getMetaData();
	    System.out.println("querying SELECT * FROM XXX");
	    int columnsNumber = rsmd.getColumnCount();
	    while (resultSet.next()) {
	        for (int i = 1; i <= columnsNumber; i++) {
	            if (i > 1) System.out.print(",  ");
	            String columnValue = resultSet.getString(i);
	            System.out.print(columnValue + " " + rsmd.getColumnName(i));
	        }
	        System.out.println("");
	    }
	}
}
