package net.luversof.web.dynamiccrud.setting.jdbc.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.luversof.web.dynamiccrud.setting.domain.Product;

public class ProductRowMapper extends SettingRowMapper<Product> {

	@Override
	public Product mapRow(ResultSet rs, int rowNum) throws SQLException {
		var product = new Product();
		product.setProduct(rs.getString("product"));
		product.setProductName(rs.getString("productName"));
		product.setOperator(rs.getString("operator"));
		product.setRegisterDate(getZonedDateTime(rs.getTimestamp("registerDate")));
		product.setModifyDate(getZonedDateTime(rs.getTimestamp("modifyDate")));
		return product;
	}

}
