package net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.luversof.web.dynamiccrud.setting.domain.Product;

public class MainMenuRowMapper extends SettingRowMapper<Product> {

	@Override
	public Product mapRow(ResultSet rs, int rowNum) throws SQLException {
		var product = new Product();
		product.setProduct(rs.getString("product"));
		product.setProduct(rs.getString("mainMenu"));
		product.setProductName(rs.getString("mainMenuName"));
		setCommon(product, rs);
		return product;
	}

}
