package net.luversof.web.dynamiccrud.setting.service;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.github.luversof.boot.jdbc.datasource.annotation.RoutingDataSource;
import net.luversof.web.dynamiccrud.setting.domain.Field;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.jdbc.mapper.mariadb.FieldRowMapper;
import net.luversof.web.dynamiccrud.setting.repository.FieldRepository;

@Service
@RoutingDataSource(SettingDataService.DATASOURCE_NAME)
public class FieldService implements SettingService<Field> {
	
	@Autowired
	private FieldRepository fieldRepository;
	
	@Autowired
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	
	@Autowired
	private SettingDataService settingDataService;
	
	private static final RowMapper<Field> ROW_MAPPER = new FieldRowMapper();

	@Override
	public Page<Field> find(SettingParameter settingParameter, Pageable pageable) {
		
		// select 쿼리 생성
		var selectQueryBuilder = new StringBuilder("SELECT product, mainMenu, subMenu, `column`, `name`, `type`, preset, `format`, `validation`, `visible`, enableSearch, enableEdit, formSize, formOrder, operator, registerDate, modifyDate FROM Fields ");
		var countQueryBuilder = new StringBuilder("SELECT COUNT(1) FROM Fields ");
		
		
		var paramSource = new MapSqlParameterSource();
		
		if (settingParameter != null) {
			var conditionQueryBuilder = new StringBuilder();
			boolean checkAlreadWhereCondition = false;
			
			if (StringUtils.hasText(settingParameter.product()) || StringUtils.hasText(settingParameter.mainMenu()) || StringUtils.hasText(settingParameter.subMenu())) {
				conditionQueryBuilder.append("WHERE ");
			}
			
			if (StringUtils.hasText(settingParameter.product())) {
				conditionQueryBuilder.append("product = :product ");
				paramSource.addValue("product", settingParameter.product());
				checkAlreadWhereCondition = true;
			}
			
			if (StringUtils.hasText(settingParameter.mainMenu())) {
				if (checkAlreadWhereCondition) {
					conditionQueryBuilder.append("AND ");
				}
				conditionQueryBuilder.append("mainMenu = :mainMenu ");
				paramSource.addValue("mainMenu", settingParameter.mainMenu());
				checkAlreadWhereCondition = true;
			}
			
			if (StringUtils.hasText(settingParameter.subMenu())) {
				if (checkAlreadWhereCondition) {
					conditionQueryBuilder.append("AND ");
				}
				conditionQueryBuilder.append("subMenu = :subMenu ");
				paramSource.addValue("subMenu", settingParameter.subMenu());
				checkAlreadWhereCondition = true;
			}
			
			selectQueryBuilder.append(conditionQueryBuilder);
			countQueryBuilder.append(conditionQueryBuilder);
		}
		
		
		selectQueryBuilder.append("LIMIT :limit OFFSET :offset");
		paramSource.addValue("limit", pageable.getPageSize());
		paramSource.addValue("offset", pageable.getOffset());
		
		// count query를 먼저 조회
		int totalCount = namedParameterJdbcTemplate.queryForObject(countQueryBuilder.toString(), paramSource, Integer.class);
		if (totalCount == 0) {
			return new PageImpl<>(Collections.emptyList(), pageable, totalCount);	
		}
		
		
		List<Field> fieldList = namedParameterJdbcTemplate.query(selectQueryBuilder.toString(), paramSource, ROW_MAPPER);
		
		return new PageImpl<>(fieldList, pageable, totalCount);
	}
	
	public List<Field> findByProductAndMainMenuAndSubMenu(String product, String mainMenu, String subMenu) {
		var fieldList = settingDataService.getFieldList().stream().filter(x -> 
			x.getProduct().equals(product) 
			&& x.getMainMenu().equals(mainMenu) 
			&& x.getSubMenu().equals(subMenu)
		).collect(Collectors.toList());
		if (fieldList.isEmpty()) {
			fieldList = fieldRepository.findByProductAndMainMenuAndSubMenu(product, mainMenu, subMenu);
		}
		return fieldList.stream().sorted(Comparator.comparing(Field::getFormOrder)).collect(Collectors.toList());
	}
}
