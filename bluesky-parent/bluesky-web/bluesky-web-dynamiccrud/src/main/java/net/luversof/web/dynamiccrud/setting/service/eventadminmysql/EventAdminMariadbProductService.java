package net.luversof.web.dynamiccrud.setting.service.eventadminmysql;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.github.luversof.boot.exception.BlueskyException;
import io.github.luversof.boot.jdbc.datasource.context.RoutingDataSourceContextHolder;
import net.luversof.web.dynamiccrud.setting.domain.Product;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb.ProductRowMapper;
import net.luversof.web.dynamiccrud.setting.service.SettingServiceSupplier;
import net.luversof.web.dynamiccrud.setting.service.eventadmin.EventAdminConstant;

@Service
public class EventAdminMariadbProductService implements SettingServiceSupplier<Product> {
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	private static final RowMapper<Product> ROW_MAPPER = new ProductRowMapper();

	@Override
	public Product findOne(SettingParameter settingParameter) {
		var product = settingParameter.product();
		if (!StringUtils.hasText(product)) {
			throw new BlueskyException("NOT_EXIST_PARAMETER_PRODUCT");
		}
		
		RoutingDataSourceContextHolder.setContext(() -> EventAdminConstant.DATASOURCE_NAME);
		
		var paramSource = new MapSqlParameterSource();
		paramSource.addValue("product", product);
		
		return namedParameterJdbcTemplate.query("SELECT * FROM Products WHERE product = :product", paramSource, ROW_MAPPER).stream().findAny().orElseGet(() -> null);
	}

}
