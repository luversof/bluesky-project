package net.luversof.web.dynamiccrud.use.service.mariadb;

import org.springframework.stereotype.Service;

import lombok.Getter;
import net.luversof.web.dynamiccrud.setting.domain.SubMenuDbType;
import net.luversof.web.dynamiccrud.use.service.AbstractDbUseService;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.Offset;
import net.sf.jsqlparser.statement.select.PlainSelect;

@Service
public class MariadbUseService extends AbstractDbUseService {
	
	@Getter
	private final SubMenuDbType supportDbType = SubMenuDbType.MySql;
	
//	@Getter
//	private String countQuery = "SELECT COUNT(1) FROM ${tableName} ${whereClause}";
//	
//	@Getter
//	private String selectPagingQuery = "SELECT * FROM ${tableName} ${whereClause} ${orderClause} ${limitClause}";
//	
//	@Getter
//	private String limitClause = "LIMIT :limit OFFSET :offset";

	@Override
	protected void addPagingCondition(PlainSelect plainSelect, int limit, long offset) {
		var limitExpression = new Limit();
		limitExpression.setRowCount(new LongValue(limit));
		plainSelect.setLimit(limitExpression);
		
		var offsetExpression = new Offset();
		offsetExpression.setOffset(new LongValue(offset));
		plainSelect.setOffset(offsetExpression);
	}
	
} 