package net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.luversof.web.dynamiccrud.setting.domain.Product;

public class ProductRowMapper extends SettingRowMapper<Product> {

	@Override
	public Product mapRow(ResultSet rs, int rowNum) throws SQLException {
		var product = new Product(
			rs.getString("product"),
			rs.getString("productName")
		);
		setCommon(product, rs);
		return product;
	}

}
