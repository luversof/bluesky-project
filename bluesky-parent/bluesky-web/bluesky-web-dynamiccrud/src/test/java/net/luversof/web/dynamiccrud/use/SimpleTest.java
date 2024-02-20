package net.luversof.web.dynamiccrud.use;

import java.util.Set;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.JoinRelType;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.tools.FrameworkConfig;
import org.apache.calcite.tools.Frameworks;
import org.apache.calcite.tools.Planner;
import org.apache.calcite.tools.RelBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.ParsedSql;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.ParseException;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.util.deparser.StatementDeParser;

@Slf4j
public class SimpleTest {

	// jsqlparser 테스트
	
	String sqlStr = "select * from dual as a where columnA=?";
	{
		sqlStr = "select * from dual as a WITH (NOLOCK) where columnA=:columnA";
//		sqlStr = "select 1";
		
		// spring-jdbc named parameter를 사용하는 경우
//		sqlStr = """
//				SELECT * FROM 
//				TableName 
//				WHERE columnA = :columnA
//				""";
//
//		// mysql offset limit 있는 경우
//		sqlStr = """
//				SELECT * FROM PROPERTIES
//				WHERE PROFILE = 'opdev'
//				LIMIT 10 OFFSET 0;
//				""";
//		
//		// mssql offset fetch 있는 경우
//		sqlStr = """
//				SELECT * FROM dbo.active_rules
//				ORDER BY uuid ASC
//				OFFSET :offset ROWS
//				FETCH NEXT :limit ROWS ONLY;
//				""";
		
		
//		sqlStr = """
//				SELECT /*+ PARALLEL */
//				    cfe.id_collateral_ref.nextval
//				    , id_collateral
//				FROM (  SELECT DISTINCT
//				            a.id_collateral
//				        FROM cfe.collateral a
//				            LEFT JOIN cfe.collateral_ref b
//				                ON a.id_collateral = b.id_collateral
//				        WHERE b.id_collateral_ref IS NULL )
//				""";
	}
	
	// spring-jdbc
	// 단순하게 namedParameter를 추출하는 기능만 있음
	@Test
	void namedParameterTest() {
		ParsedSql sqlStatement = NamedParameterUtils.parseSqlStatement(sqlStr);
		log.debug("sqlStatement : {}", sqlStatement);
	}
	
	// spring-data-jpa
	@Test
	void queryUtilsTest() {
		
		// deprecated 됨 DeclaredQuery 를 대신 쓰라고 주석에 달아놓고 왜 public이 아닌것인가...
		// 다만 count 쿼리 생성을 해주는 점은 괜찮아보이긴 함.
		// LIMIT OFFSET 뗴고 이거 호출해도 되긴 할 듯?
		String countQueryFor = QueryUtils.createCountQueryFor(sqlStr);
		log.debug("countQueryFor : {}", countQueryFor);
		
		
		// order 이후 조건들이 있으면 작성이 문제 있음.
		// 적당히 자른다면 쓸 수 있을지도?
		String applySorting = QueryUtils.applySorting(sqlStr, Sort.by(Order.asc("test")));
		log.debug("applySorting : {}", applySorting);
		
		// QueryUtils에 QueryEnhancer 호출이 안보이긴한데 QueryEnhancer를 통해 jsqlparser도 사용이 가능한 듯 함
		
		// spring data jpa에 JSqlParserUtils가 있긴 하지만 별 기능 없음
		
		// ExpressionBasedStringQuery를 살펴보면 query 에 SpEL을 사용한 경우에 대한 지원이 제공됨 
	}
	
	//jsqlparser
	@Test
	void jsqlParserTest() throws JSQLParserException {
		
		Statement statement = CCJSqlParserUtil.parse(sqlStr);
		log.debug("test : {}", statement.getClass());
		
		PlainSelect select = null;
		if (statement instanceof PlainSelect) {
			select = (PlainSelect) statement;
		}
		if (select == null) {
			return;
		}
		
		SelectItem selectItem =
		        select.getSelectItems().get(0);
		log.debug("test : {}", selectItem);
		
		Table table = (Table) select.getFromItem();
		log.debug("table : {}", table);
		log.debug("table : {}", table.getName());
	
		
		StringBuilder builder = new StringBuilder();
		StatementDeParser deParser = new StatementDeParser(builder);
		deParser.visit(select);
		
		log.debug("test : {}", builder.toString());
		
	}
	
	@Test
	void getTableNameTest() throws JSQLParserException {
		Set<String> tables = TablesNamesFinder.findTables(sqlStr);
		log.debug("tableNames : {}", tables.isEmpty());
		if (!tables.isEmpty()) {
			log.debug("tableNames : {}", tables.stream().findFirst().get());
		}
	}
	
	// 특정 조건으로 만들어 보자.
	
	// apache calcite
	@Test
	void calciteTest() throws SqlParseException {
		FrameworkConfig frameworkConfig = Frameworks.newConfigBuilder().build();
		
		
//		RelBuilder relBuilder = RelBuilder.create(frameworkConfig);
//		
//		RelNode relNode = relBuilder. scan("DEPT")
//        .scan("EMP")
//        .join(
//            JoinRelType.ANTI, relBuilder.equals(
//            		relBuilder.field(2, 1, "DEPTNO"),
//            		relBuilder.field(2, 0, "DEPTNO")))
//        .project(relBuilder.field("DEPTNO"))
//        .build();
//		log.debug("relNode : {}", relNode);
		
		Planner planner = Frameworks.getPlanner(frameworkConfig);
		SqlNode sqlNode = planner.parse(sqlStr);
		log.debug("sqlNode : {}", sqlNode.toString());
	}
}
