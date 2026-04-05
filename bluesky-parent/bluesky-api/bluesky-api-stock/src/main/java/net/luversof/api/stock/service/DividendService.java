package net.luversof.api.stock.service;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.luversof.api.stock.constant.StockErrorCode;
import net.luversof.api.stock.domain.Dividend;
import net.luversof.api.stock.domain.StockItem;
import net.luversof.api.stock.web.dto.request.DividendSearchRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
public class DividendService {

    private final JdbcClient jdbcClient;
    private final StockItemService stockItemService;

    public DividendService(
            @Qualifier("stockJdbcClient") JdbcClient jdbcClient,
            StockItemService stockItemService) {
        this.jdbcClient = jdbcClient;
        this.stockItemService = stockItemService;
    }

    @Transactional(readOnly = true)
    public List<Dividend> findDividends(DividendSearchRequest request) {
        if (request.getUserId() == null) {
            StockErrorCode.NOT_EXIST_USER_ID.throwException();
        }

        StringBuilder sql =
                new StringBuilder()
                        .append(
                                "SELECT d.\"id\" as \"id\", d.\"account_id\" as \"accountId\", d.\"stockItem_id\" as \"stockItemId\", si.\"name\" as \"stockItemName\", d.\"type\" as \"type\", d.\"quantity\" as \"quantity\", ")
                        .append(
                                "d.\"amountPerShare\" as \"amountPerShare\", d.\"taxPerShare\" as \"taxPerShare\", d.\"grossAmount\" as \"grossAmount\", ")
                        .append(
                                "d.\"fee\" as \"fee\", d.\"tax\" as \"tax\", d.\"taxableAmount\" as \"taxableAmount\", d.\"recordDate\" as \"recordDate\", d.\"payDate\" as \"payDate\" ")
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
            params.put("startDate", Timestamp.from(request.getStartDate()));
        }

        if (request.getEndDate() != null) {
            sql.append(" AND d.\"payDate\" <= :endDate");
            params.put("endDate", Timestamp.from(request.getEndDate()));
        }

        sql.append(" ORDER BY d.\"payDate\" DESC, d.\"id\" DESC");

        var result = jdbcClient.sql(sql.toString()).params(params).query(Dividend.class).list();

        // Fill missing stockItemId by looking up StockItem by name
        for (var dividend : result) {
            if (dividend != null
                    && dividend.getStockItemId() == null
                    && dividend.getStockItemName() != null) {
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
