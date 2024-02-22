package net.luversof.web.dynamiccrud.use.service.mssql;

import org.springframework.stereotype.Service;

import lombok.Getter;
import net.luversof.web.dynamiccrud.setting.domain.SubMenuDbType;
import net.luversof.web.dynamiccrud.use.service.AbstractDbUseService;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.statement.select.Fetch;
import net.sf.jsqlparser.statement.select.Offset;
import net.sf.jsqlparser.statement.select.PlainSelect;

@Service
public class MssqlUseService extends AbstractDbUseService {
	
	@Getter
	private final SubMenuDbType supportDbType = SubMenuDbType.MsSql;
	
//	@Getter
//	private final String countQuery = "SELECT COUNT(1) FROM ${tableName} WITH (READUNCOMMITTED) ${whereClause}";
//	
//	@Getter
//	private final String selectPagingQuery = """
//			SELECT * 
//			FROM ${tableName} WITH (READUNCOMMITTED)
//			${whereClause}
//			${orderClause}
//			${limitClause}
//			""";
//	@Getter
//	private String limitClause = """
//			OFFSET :offset ROWS
//			FETCH NEXT :limit ROWS ONLY;
//			""";
	@Override
	protected void addPagingCondition(PlainSelect plainSelect, int limit, long offset) {
		var fetchExpression = new Fetch();
		fetchExpression.setExpression(new LongValue(limit));
		fetchExpression.addFetchParameter("ROWS");
		fetchExpression.addFetchParameter("ONLY");
		plainSelect.setFetch(fetchExpression);
		
		var offsetExpression = new Offset();
		offsetExpression.setOffset(new LongValue(offset));
		offsetExpression.setOffsetParam("ROWS");
		plainSelect.setOffset(offsetExpression);
	}

}
 