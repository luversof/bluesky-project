package net.luversof.web.dynamiccrud.use;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hibernate.engine.jdbc.internal.BasicFormatterImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.luversof.web.dynamiccrud.setting.util.JSqlParserUtil;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.Fetch;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.Offset;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;

@Slf4j
public class JSqlParserTest {

	// jsqlparser 테스트
	
//	String sqlStr = "select * from dual as a where columnA=?";
//	{
//		sqlStr = "select * from dual WITH (NOLOCK) where columnA=:columnA AND columnB = :columnB AND columnC = :columnC";
//		sqlStr = "select 1";
//		
//		// spring-jdbc named parameter를 사용하는 경우
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
//		
//		
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
//	}
//	
//	// spring-jdbc
//	// 단순하게 namedParameter를 추출하는 기능만 있음
//	@Test
//	void namedParameterTest() {
//		ParsedSql sqlStatement = NamedParameterUtils.parseSqlStatement(sqlStr);
//		log.debug("sqlStatement : {}", sqlStatement);
//	}
//	
//	// spring-data-jpa
//	@Test
//	void queryUtilsTest() {
//		
//		// deprecated 됨 DeclaredQuery 를 대신 쓰라고 주석에 달아놓고 왜 public이 아닌것인가...
//		// 다만 count 쿼리 생성을 해주는 점은 괜찮아보이긴 함.
//		// LIMIT OFFSET 뗴고 이거 호출해도 되긴 할 듯?
//		@SuppressWarnings("deprecation")
//		String countQueryFor = QueryUtils.createCountQueryFor(sqlStr);
//		log.debug("countQueryFor : {}", countQueryFor);
//		
//		
//		// order 이후 조건들이 있으면 작성이 문제 있음.
//		// 적당히 자른다면 쓸 수 있을지도?
//		String applySorting = QueryUtils.applySorting(sqlStr, Sort.by(Order.asc("test")));
//		log.debug("applySorting : {}", applySorting);
//		
//		// QueryUtils에 QueryEnhancer 호출이 안보이긴한데 QueryEnhancer를 통해 jsqlparser도 사용이 가능한 듯 함
//		
//		// spring data jpa에 JSqlParserUtils가 있긴 하지만 별 기능 없음
//		
//		// ExpressionBasedStringQuery를 살펴보면 query 에 SpEL을 사용한 경우에 대한 지원이 제공됨 
//	}
//	
//	@SneakyThrows
//	@Test
//	void getTableNameTest() {
//		Set<String> tables = TablesNamesFinder.findTables(sqlStr);
//		log.debug("tableNames : {}", tables.isEmpty());
//		if (!tables.isEmpty()) {
//			log.debug("tableNames : {}", tables.stream().findFirst().get());
//		}
//	}
	
	//jsqlparser
	@Test
	void jSqlParserTest() throws JSQLParserException {
		String sqlStr;
//		sqlStr = "select count(*) from dual as a where columnA=? limit 10 offset 20";
		sqlStr = "select count(*) from dual as a where `columnA` = ? limit 10 offset 20";
//		sqlStr = """
//				SELECT 
//				* 
//				FROM dual AS AAA WITH (NOLOCK)  
//				WHERE `columnA` = :columnA and columnB like '%' + :columnB + '%' AND columnC = :columnC
//				ORDER BY AAA ASC
//				OFFSET 11 ROWS
//				FETCH NEXT 22 ROWS ONLY
//				""";
//		sqlStr = """
//				SELECT 
//				* 
//				FROM dual AS AAA WITH (NOLOCK)  
//				WHERE columnB like :columnB + '%'
//				ORDER BY AAA ASC
//				OFFSET 11 ROWS
//				FETCH NEXT 22 ROWS ONLY
//				""";
		
		
		var plainSelect = (PlainSelect) CCJSqlParserUtil.parse(sqlStr);
		
		Table table = (Table) plainSelect.getFromItem();
		log.debug("table : {}", table);
		log.debug("table : {}", table.getName());
		
		if (plainSelect.getWhere() instanceof BinaryExpression where && where.getLeftExpression() instanceof Column column) {
			log.debug("where column : {}", column.getColumnName());
			log.debug("where column : {}", column.getFullyQualifiedName());
			log.debug("where column : {}", column.getFullyQualifiedName());
		}
	
		log.debug("test : {}", plainSelect.toString());
		
	}
	
	
	@SneakyThrows
	@ParameterizedTest
	@EnumSource(
			value = QueryCaseEnum.class,
			names = {
//					"페이징쿼리"
					}
	)
	void jSqlParserTest(QueryCaseEnum queryCaseEnum) {
		for (var queryCase : queryCaseEnum.getQueryCaseList()) {
				log.debug("targetQuery : {}, {}, {}", queryCaseEnum.name(), queryCase.getDbType(), queryCase.getQueryStr());
				
				
				// 추가 조건 dataMap
				var whereClauseMap = new LinkedHashMap<String, String>();
				whereClauseMap.put("addWhereColumnA", "addWhereColumnAValue");
				whereClauseMap.put("addWhereColumnB", "addWhereColumnBValue");
				
				// count Query 생성
				{
					var plainSelect = (PlainSelect) CCJSqlParserUtil.parse(queryCase.getQueryStr());
					
					
					plainSelect.getSelectItems().clear();
					var function = new Function();
					function.setName("count");
					function.setParameters(new AllColumns());
					plainSelect.getSelectItems().add(new SelectItem<Function>(function));
					
					calculatewhereClause(plainSelect, whereClauseMap);
					
					plainSelect.setOrderByElements(null);
					
//					log.debug("origin query : {}", entry.getValue());
					
					// count 쿼리 생성시엔 기존 쿼리에서 몇몇 조건을 체크해서 지워야 할 수도 있음
					
					log.debug("generate count query : {}", plainSelect.toString());
				}
				
				
				// (s) limit offset Query 생성
				if (queryCase.getDbType() == QueryCaseDbType.MARIADB)
				{
					//이거 재활용 못하나?;;;
					var plainSelect = (PlainSelect) CCJSqlParserUtil.parse(queryCase.getQueryStr());
					var limit = new Limit();
					limit.setRowCount(new LongValue(10));
					plainSelect.setLimit(limit);
					
					var offset = new Offset();
					offset.setOffset(new LongValue(20));
					plainSelect.setOffset(offset);
					
					calculatewhereClause(plainSelect, whereClauseMap);
					
					log.debug("generate paging query : {}", plainSelect.toString());
				}
				if (queryCase.getDbType() == QueryCaseDbType.MSSQL)
				{
					//이거 재활용 못하나?;;;
					var plainSelect = (PlainSelect) CCJSqlParserUtil.parse(queryCase.getQueryStr());
					var fetch = new Fetch();
					fetch.setExpression(new LongValue(10));
					fetch.addFetchParameter("ROWS");
					fetch.addFetchParameter("ONLY");
					plainSelect.setFetch(fetch);
					
					var offset = new Offset();
					offset.setOffset(new LongValue(20));
					offset.setOffsetParam("ROWS");
					plainSelect.setOffset(offset);
					
					calculatewhereClause(plainSelect, whereClauseMap);
					
					log.debug("generate paging query : {}", plainSelect.toString());
				}
				// (e) limit offset Query 생성
				// 추가 where 조건이 있으면 추가 처리
				// 단일이면 바로 추가, 복수면 AndExpresion 하위에 계층구조로 추가
		}
		
	}
	
	private void calculatewhereClause(PlainSelect plainSelect, Map<String, String> dataMap) {
		
		BinaryExpression targetExpression = (BinaryExpression) plainSelect.getWhere();
		
		for (var key : dataMap.keySet()) {
			var appendExpression = createEqualsToExpression(key, key);
			if (targetExpression== null) {
				targetExpression = appendExpression;
			} else {
				targetExpression = appendWhereExpression(targetExpression, appendExpression);
			}
		}
		
		plainSelect.setWhere(targetExpression);
		
	}
	
	private BinaryExpression appendWhereExpression(BinaryExpression targetExpression, BinaryExpression appendExpression) {
		Set<Column> set = new HashSet<>();
		
		// 기존에 해당 컬럼에 대한 조건이 있으면 추가하지 않음
		targetExpression.accept(new ExpressionVisitorAdapter() {

			@Override
			public void visit(Column column) {
				
				if (appendExpression.getLeftExpression() instanceof Column appendColumn && column.getColumnName().equals(appendColumn.getColumnName())) {
//					System.out.println("중복 컬럼 있음 " + column.getColumnName());
					set.add(column);
				} else {
//					System.out.println("중복 컬럼 없음");
				}
				
			}
			
		});
		
		return set.isEmpty() ? new AndExpression(targetExpression, appendExpression) : targetExpression;
	}
	
	private BinaryExpression createEqualsToExpression(String key, String value) {
		return new EqualsTo(new Column(key), new JdbcNamedParameter(value));
	}
	

	@SneakyThrows
	@ParameterizedTest
	@EnumSource(
			value = QueryCaseEnum.class,
			names = {
//					"단순쿼리_Where조건추가",
//					"단순쿼리_Where조건추가2",
//					"단순쿼리_Where조건추가3",
//					"단순쿼리_Where조건추가4",
					"컬럼삭제케이스예제"
					}
	)
	void removeWhereClauseByColumnNameTest(QueryCaseEnum queryCaseEnum) {
		for (var queryCase : queryCaseEnum.getQueryCaseList()) {
			String sqlStr = queryCase.getQueryStr();
			
			List<String> targetColumnList = List.of("columnA" , "columnB", "columnC",  "columnD", "columnE" );
			
			for (var targetColumn : targetColumnList) {
				var plainSelect = (PlainSelect) CCJSqlParserUtil.parse(sqlStr);
				JSqlParserUtil.removeWhereClauseByColumnName(plainSelect, targetColumn);
				
				log.debug("""
						{}
						{}
						""",
						queryCaseEnum.name(), 
						formatSql(sqlStr));
				log.debug("Delete Column : {}", targetColumn);
				log.debug("""
						Query Result : 
						{}
						""",
						formatSql(plainSelect.toString()));
			}
			
		}
	}
	
	@SneakyThrows
	@ParameterizedTest
	@EnumSource(
			value = QueryCaseEnum.class,
			names = {
//					"단순쿼리_Where조건추가4",
//					"컬럼삭제케이스예제",
//					"컬럼삭제케이스고정값예제",
//					"컬럼삭제케이스복합조건예제",
//					"동일NamedParameter중첩사용"
					}
	)
	void removeWhereClauseByNamedParameterNameTest(QueryCaseEnum queryCaseEnum) {
		for (var queryCase : queryCaseEnum.getQueryCaseList()) {
			String sqlStr = queryCase.getQueryStr();
			
			List<String> targetColumnList = List.of(
					"columnA" 
//					, "columnB" 
//					, "columnD"
			);
			
			for (var targetColumn : targetColumnList) {
				var plainSelect = (PlainSelect) CCJSqlParserUtil.parse(sqlStr);
				JSqlParserUtil.removeWhereClauseByNamedParameterName(plainSelect, targetColumn);
				
				log.debug("""
						{}
						{}
						""",
						queryCaseEnum.name(), 
						formatSql(sqlStr));
				log.debug("Delete Column : {}", targetColumn);
				log.debug("""
						Query Result : 
						{}
						""",
						formatSql(plainSelect.toString()));
			}
		}
	}
	
	
	@SneakyThrows
	@ParameterizedTest
	@EnumSource(
			value = QueryCaseEnum.class,
			names = {
					"컬럼삭제케이스예제",
					"컬럼삭제케이스고정값예제",
					"컬럼삭제케이스복합조건예제"
					}
	)
	void findWhereClauseRightExpressionByColumnNameTest(QueryCaseEnum queryCaseEnum) {
		for (var queryCase : queryCaseEnum.getQueryCaseList()) {
			String sqlStr = queryCase.getQueryStr();
			
			List<String> targetColumnList = List.of("columnB", "columnC", "columnD");
			
			for (var targetColumn : targetColumnList) {
				var plainSelect = (PlainSelect) CCJSqlParserUtil.parse(sqlStr);
				Expression whereColumnRightExpression = JSqlParserUtil.findWhereClauseRightExpressionByColumnName(plainSelect, targetColumn);
				
				log.debug("""
						{}
						{}
						""",
						queryCaseEnum.name(), 
						formatSql(sqlStr));
				log.debug("target Column : {}", targetColumn);
				log.debug("""
						target Column rightExpression class : {} 
						{}
						""",
						whereColumnRightExpression.getClass().getSimpleName(),
						formatSql(whereColumnRightExpression.toString()));
			}
			
		}
	}
	
	@SneakyThrows
	@ParameterizedTest
	@EnumSource(
			value = QueryCaseEnum.class,
			names = {
					"단순쿼리",
					"컬럼삭제케이스예제",
					"컬럼삭제케이스고정값예제",
					"컬럼삭제케이스복합조건예제"
					}
	)
	void findWhereClauseRightExpressionContainsTypeListByColumnNameTest(QueryCaseEnum queryCaseEnum) {
		for (var queryCase : queryCaseEnum.getQueryCaseList()) {
			String sqlStr = queryCase.getQueryStr();
			
			List<String> targetColumnList = List.of("columnB", "columnC", "columnD", "없는값");
			
			for (var targetColumn : targetColumnList) {
				var plainSelect = (PlainSelect) CCJSqlParserUtil.parse(sqlStr);
				
				List<String> whereColumnRightExpressionTypeList = JSqlParserUtil.findWhereClauseRightExpressionContainsTypeListByColumnName(plainSelect, targetColumn);
				
				log.debug("""
						{}
						{}
						""",
						queryCaseEnum.name(), 
						formatSql(sqlStr));
				log.debug("target Column : {}", targetColumn);
				log.debug("""
						target Column rightExpression class : {}
						{}
						""",
						whereColumnRightExpressionTypeList.getClass().getSimpleName(),
						formatSql(whereColumnRightExpressionTypeList.toString()));
			}
			
		}
	}
		
	@SneakyThrows
	@ParameterizedTest
	@EnumSource(
			value = QueryCaseEnum.class,
			names = {
					"단순쿼리",
					"컬럼삭제케이스예제",
					"컬럼삭제케이스고정값예제",
					"컬럼삭제케이스복합조건예제"
					}
	)
	void findWhereClauseColumnNameListTest(QueryCaseEnum queryCaseEnum) {
		for (var queryCase : queryCaseEnum.getQueryCaseList()) {
			String sqlStr = queryCase.getQueryStr();
			var plainSelect = (PlainSelect) CCJSqlParserUtil.parse(sqlStr);
			List<String> whereClauseColumnList = JSqlParserUtil.findWhereClauseColumnNameList(plainSelect);

			log.debug("""
					{}
					{}
					""",
					queryCaseEnum.name(), 
					formatSql(sqlStr));
			log.debug("target whereClauseColumnList : {}", whereClauseColumnList);
		}
	}
		
	@SneakyThrows
	@ParameterizedTest
	@EnumSource(
			value = QueryCaseEnum.class,
			names = {
					"단순쿼리",
					"컬럼삭제케이스예제",
					"컬럼삭제케이스고정값예제",
					"컬럼삭제케이스복합조건예제"
					}
	)
	void findWhereClauseNamedParameterNameListTest(QueryCaseEnum queryCaseEnum) {
		for (var queryCase : queryCaseEnum.getQueryCaseList()) {
			String sqlStr = queryCase.getQueryStr();
			var plainSelect = (PlainSelect) CCJSqlParserUtil.parse(sqlStr);
			List<String> whereClauseNamedParameterNameList = JSqlParserUtil.findWhereClauseNamedParameterNameList(plainSelect);
			
			log.debug("""
					{}
					{}
					""",
					queryCaseEnum.name(), 
					formatSql(sqlStr));
			log.debug("target whereClauseNamedParameterNameList : {}", whereClauseNamedParameterNameList);
		}
	}

	
	// 특정 조건으로 만들어 보자.
	
	// apache calcite
//	@Test
//	void calciteTest() throws SqlParseException {
//		FrameworkConfig frameworkConfig = Frameworks.newConfigBuilder().build();
//		
//		
////		RelBuilder relBuilder = RelBuilder.create(frameworkConfig);
////		
////		RelNode relNode = relBuilder. scan("DEPT")
////        .scan("EMP")
////        .join(
////            JoinRelType.ANTI, relBuilder.equals(
////            		relBuilder.field(2, 1, "DEPTNO"),
////            		relBuilder.field(2, 0, "DEPTNO")))
////        .project(relBuilder.field("DEPTNO"))
////        .build();
////		log.debug("relNode : {}", relNode);
//		
//		Planner planner = Frameworks.getPlanner(frameworkConfig);
//		SqlNode sqlNode = planner.parse(sqlStr);
//		log.debug("sqlNode : {}", sqlNode.toString());
//	}
	
	private String formatSql(String sql) {
		return new BasicFormatterImpl().format(sql);
	}
}
