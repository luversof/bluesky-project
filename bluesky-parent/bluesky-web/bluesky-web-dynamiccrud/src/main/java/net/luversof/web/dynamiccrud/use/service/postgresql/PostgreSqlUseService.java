package net.luversof.web.dynamiccrud.use.service.postgresql;

import org.springframework.stereotype.Service;

import net.luversof.web.dynamiccrud.setting.domain.SubMenuDbType;
import net.luversof.web.dynamiccrud.use.service.AbstractDbUseService;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.Offset;
import net.sf.jsqlparser.statement.select.PlainSelect;

@Service
public class PostgreSqlUseService extends AbstractDbUseService {

    private final SubMenuDbType supportDbType = SubMenuDbType.PostgreSql;

    public SubMenuDbType getSupportDbType() {
        return supportDbType;
    }

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
