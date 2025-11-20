package net.luversof.api.stock.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import net.luversof.api.stock.constant.StockErrorCode;
import net.luversof.api.stock.domain.Dividend;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.web.dto.request.DividendSearchRequest;

@Service
public class DividendService {

	private final NamedParameterJdbcOperations jdbcOperations;
	private final StockItemService stockItemService;

	public DividendService(
			@Qualifier("stockNamedParameterJdbcOperations") NamedParameterJdbcOperations jdbcOperations,
			StockItemService stockItemService) {
		this.jdbcOperations = jdbcOperations;
		this.stockItemService = stockItemService;
	}

	private static final BeanPropertyRowMapper<Dividend> ROW_MAPPER = new BeanPropertyRowMapper<>(Dividend.class);

	@Transactional(readOnly = true)
	public List<Dividend> findDividends(DividendSearchRequest request) {
		if (request.getUserId() == null) {
			StockErrorCode.NOT_EXIST_USER_ID.throwException();
		}

		StringBuilder sql = new StringBuilder()
				.append("SELECT d.\"id\" as \"id\", d.\"account_id\" as \"accountId\", d.\"stockItem_id\" as \"stockItemId\", si.\"name\" as \"stockItemName\", d.\"type\" as \"type\", d.\"quantity\" as \"quantity\", d.\"price\" as \"price\", ")
				.append("d.\"fee\" as \"fee\", d.\"tax\" as \"tax\", d.\"recordDate\" as \"recordDate\", d.\"payDate\" as \"payDate\" ")
				.append("FROM \"Dividend\" d ")
				.append("JOIN \"Account\" a ON d.\"account_id\" = a.\"id\" ")
				.append("LEFT JOIN \"StockItem\" si ON d.\"stockItem_id\" = si.\"id\" ")
				.append("WHERE a.\"user_id\" = :userId");

		Map<String, Object> params = new HashMap<>();
		params.put("userId", request.getUserId());

		if (!CollectionUtils.isEmpty(request.getAccountIdList())) {
			sql.append(" AND d.\"account_id\" IN (:accountIdList)");
			params.put("accountIdList", request.getAccountIdList());
		}

		if (!CollectionUtils.isEmpty(request.getStockItemIdList())) {
			sql.append(" AND d.\"stockItem_id\" IN (:stockItemIdList)");
			params.put("stockItemIdList", request.getStockItemIdList());
		}

		if (request.getStartDate() != null) {
			sql.append(" AND d.\"payDate\" >= :startDate");
			params.put("startDate", request.getStartDate());
		}

		if (request.getEndDate() != null) {
			sql.append(" AND d.\"payDate\" <= :endDate");
			params.put("endDate", request.getEndDate());
		}

		sql.append(" ORDER BY d.\"payDate\" DESC, d.\"id\" DESC");

		var result = jdbcOperations.query(sql.toString(), params, ROW_MAPPER);

		// Fill missing stockItemId by looking up StockItem by name
		for (var dividend : result) {
			if (dividend.getStockItemId() == null && dividend.getStockItemName() != null) {
				try {
					StockItem si = stockItemService.findByName(dividend.getStockItemName());
					if (si != null && si.getId() != null) {
						dividend.setStockItemId(si.getId());
					}
				} catch (Exception ignored) {
					// ignore lookup failures
				}
			}
		}

		return result;
	}
}
