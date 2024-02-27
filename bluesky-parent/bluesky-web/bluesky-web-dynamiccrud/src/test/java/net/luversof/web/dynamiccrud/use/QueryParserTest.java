package net.luversof.web.dynamiccrud.use;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.jdbc.core.namedparam.NamedParameterUtils;
import org.springframework.jdbc.core.namedparam.ParsedSql;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
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
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;

@Slf4j
public class QueryParserTest {

	// jsqlparser 테스트
	
	String sqlStr = "select * from dual as a where columnA=?";
	{
		sqlStr = "select * from dual WITH (NOLOCK) where columnA=:columnA AND columnB = :columnB AND columnC = :columnC";
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
		@SuppressWarnings("deprecation")
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
	void jSqlParserTest() throws JSQLParserException {
//		sqlStr = "select count(*) from dual as a where columnA=? limit 10 offset 20";
		sqlStr = """
				SELECT 
				* 
				FROM dual AS AAA WITH (NOLOCK)  
				WHERE columnA = :columnA and columnB like '%' + :columnB + '%' AND columnC = :columnC
				ORDER BY AAA ASC
				OFFSET 11 ROWS
				FETCH NEXT 22 ROWS ONLY
				""";
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
	
		
		StringBuilder builder = new StringBuilder();
		StatementDeParser deParser = new StatementDeParser(builder);
		deParser.visit(plainSelect);
		
		log.debug("test : {}", builder.toString());
		
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
				var whereConditionMap = new LinkedHashMap<String, String>();
				whereConditionMap.put("addWhereColumnA", "addWhereColumnAValue");
				whereConditionMap.put("addWhereColumnB", "addWhereColumnBValue");
				
				// count Query 생성
				{
					var plainSelect = (PlainSelect) CCJSqlParserUtil.parse(queryCase.getQueryStr());
					
					
					plainSelect.getSelectItems().clear();
					var function = new Function();
					function.setName("count");
					function.setParameters(new AllColumns());
					plainSelect.getSelectItems().add(new SelectItem<Function>(function));
					
					calculateWhereCondition(plainSelect, whereConditionMap);
					
					plainSelect.setOrderByElements(null);
					
					StringBuilder builder = new StringBuilder();
					StatementDeParser deParser = new StatementDeParser(builder);
					deParser.visit(plainSelect);
//					log.debug("origin query : {}", entry.getValue());
					
					// count 쿼리 생성시엔 기존 쿼리에서 몇몇 조건을 체크해서 지워야 할 수도 있음
					
					log.debug("generate count query : {}", builder.toString());
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
					
					calculateWhereCondition(plainSelect, whereConditionMap);
					
					StringBuilder builder = new StringBuilder();
					StatementDeParser deParser = new StatementDeParser(builder);
					deParser.visit(plainSelect);
					log.debug("generate paging query : {}", builder.toString());
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
					
					calculateWhereCondition(plainSelect, whereConditionMap);
					
					StringBuilder builder = new StringBuilder();
					StatementDeParser deParser = new StatementDeParser(builder);
					deParser.visit(plainSelect);
					log.debug("generate paging query : {}", builder.toString());
				}
				// (e) limit offset Query 생성
				// 추가 where 조건이 있으면 추가 처리
				// 단일이면 바로 추가, 복수면 AndExpresion 하위에 계층구조로 추가
				
			
		}
		
	}
	
	private void calculateWhereCondition(PlainSelect plainSelect, Map<String, String> dataMap) {
		
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
					"컬럼삭제케이스예제"
					}
	)
	void jSqlParserWhereColumnRemoveTest(QueryCaseEnum queryCaseEnum) {
		for (var queryCase : queryCaseEnum.getQueryCaseList()) {
			String sqlStr = queryCase.getQueryStr();
			
			String targetColumn = "columnEerer";
			
			// 대상 컬럼에 대해 처리 작업을 해야 함.
			// 트리 구조에서 대상 컬럼 탐색 후 처리
			var plainSelect = (PlainSelect) CCJSqlParserUtil.parse(sqlStr);
			
			// 아래 처럼 처리 하여 조건 제거 되는 부분 확인하였음
//		where.setLeftExpression(whereLeftExpression.getLeftExpression());
//		log.debug("whereLeftExpression : {}", whereLeftExpression);
			
			// 우선 컬럼 제거 부터 테스트 해보자.
			removeWhereClauseByColumnName(plainSelect, targetColumn);
			
			
			StringBuilder builder = new StringBuilder();
			StatementDeParser deParser = new StatementDeParser(builder);
			deParser.visit(plainSelect);
			
			
			
			log.debug("Delete Column : {}", targetColumn);
			log.debug("Query Result : {}", builder.toString());
		}
	}
	
	/**
	 * where조건의 column 제거
	 * @param plainSelect
	 * @param columnName
	 */
	private void removeWhereClauseByColumnName(PlainSelect plainSelect, String columnName) {
		if (plainSelect.getWhere() == null) {
			return;
		}
		
		
		var whereCondition = (BinaryExpression) plainSelect.getWhere();
		
		// 바로 하위 자식이 대상인 경우 체크
		if (whereCondition.getLeftExpression() instanceof Column column
				&& column.getColumnName().equals(columnName)) {
			plainSelect.setWhere(null);
			return;
		}
		
		// rightExpression이 대상인 경우 체크
		if (whereCondition.getRightExpression() instanceof BinaryExpression rightExpression 
				&& rightExpression.getLeftExpression() instanceof Column column
				&& column.getColumnName().equals(columnName)) {
			plainSelect.setWhere(whereCondition.getLeftExpression());
			return;
		}
		
		// 위 두 조건이 아니면 하위 자식의 자식을 순환 체크
		removeWhereClauseByColumnNameNested(whereCondition, columnName);
		
	}
	
	
	private void removeWhereClauseByColumnNameNested(BinaryExpression superExpression, String columnName) {
		if (!(superExpression.getLeftExpression() instanceof BinaryExpression)) {
			return;
		}
		var targetExpression = (BinaryExpression) superExpression.getLeftExpression();
		
		//leftExpression이 대상인 경우 rightExpression을 상위 leftExpression으로 올림
		if (targetExpression.getLeftExpression() instanceof Column column
				&& column.getColumnName().equals(columnName)) {
			superExpression.setLeftExpression(targetExpression.getRightExpression());
			return;
		}
		
		// rightExpression 하위가 대상인 경우 leftExpression을 상위 leftExpression으로 올림
		if (targetExpression.getRightExpression() instanceof BinaryExpression rightExpression 
				&& rightExpression.getLeftExpression() instanceof Column column
				&& column.getColumnName().equals(columnName)) {
			superExpression.setLeftExpression(targetExpression.getLeftExpression());
			return;
		}

		// 아니면 중첩 호출로 leftExpression 탐색
		removeWhereClauseByColumnNameNested((BinaryExpression) superExpression.getLeftExpression(), columnName);
		log.debug("test : {}");
		
		
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
	void jSqlParserWhereRightExpressionConditionTest(QueryCaseEnum queryCaseEnum) {
		for (var queryCase : queryCaseEnum.getQueryCaseList()) {
			String sqlStr = queryCase.getQueryStr();
			
			List<String> targetColumnList = List.of("columnB", "columnC", "columnD");
			
			for (var targetColumn : targetColumnList) {
				// 대상 컬럼에 대해 처리 작업을 해야 함.
				// 트리 구조에서 대상 컬럼 탐색 후 처리
				var plainSelect = (PlainSelect) CCJSqlParserUtil.parse(sqlStr);
				
				
				// 우선 컬럼 제거 부터 테스트 해보자.
				Expression whereColumnRightExpression = findWhereClauseRightExpression(plainSelect, targetColumn);
				
				StringBuilder builder = new StringBuilder();
				StatementDeParser deParser = new StatementDeParser(builder);
				deParser.visit(plainSelect);
				
				
				
				log.debug("Query Result : {}", builder.toString());
				log.debug("target Column : {}", targetColumn);
				log.debug("target Column rightExpression : {}, {}", whereColumnRightExpression.getClass(), whereColumnRightExpression);
			}
			
		}
	}
	
	
	private Expression findWhereClauseRightExpression(PlainSelect plainSelect, String columnName) {
		if (plainSelect.getWhere() == null) {
			return null;
		}
		
		
		var whereCondition = (BinaryExpression) plainSelect.getWhere();
		
		// 바로 하위 자식이 대상인 경우 체크
		if (whereCondition.getLeftExpression() instanceof Column column
				&& column.getColumnName().equals(columnName)) {
			return whereCondition.getRightExpression();
		}
		
		// rightExpression이 대상인 경우 체크
		if (whereCondition.getRightExpression() instanceof BinaryExpression rightExpression 
				&& rightExpression.getLeftExpression() instanceof Column column
				&& column.getColumnName().equals(columnName)) {
			return rightExpression.getRightExpression();
		}
		
		// 아니면 중첩 호출
		return findWhereClauseRightExpressionNested(whereCondition, columnName);
	}
	
	private Expression findWhereClauseRightExpressionNested(BinaryExpression superExpression, String columnName) {
		if (!(superExpression.getLeftExpression() instanceof BinaryExpression)) {
			return null;
		}
		var targetExpression = (BinaryExpression) superExpression.getLeftExpression();
		
		//leftExpression이 대상인 경우 rightExpression을 반환
		if (targetExpression.getLeftExpression() instanceof Column column
				&& column.getColumnName().equals(columnName)) {
			return targetExpression.getRightExpression();
		}
		
		// rightExpression 하위가 대상인 경우 rightEsxpression 하위의 rightExpression을 반환
		if (targetExpression.getRightExpression() instanceof BinaryExpression rightExpression 
				&& rightExpression.getLeftExpression() instanceof Column column
				&& column.getColumnName().equals(columnName)) {
			return rightExpression.getRightExpression();
		}
		
		// 아니면 중첩 호출로 leftExpression 탐색
		return findWhereClauseRightExpressionNested((BinaryExpression) superExpression.getLeftExpression(), columnName);
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
	void jSqlParserWhereRightExpressionTypeTest(QueryCaseEnum queryCaseEnum) {
		for (var queryCase : queryCaseEnum.getQueryCaseList()) {
			String sqlStr = queryCase.getQueryStr();
			
			List<String> targetColumnList = List.of("columnB", "columnC", "columnD", "없는값");
			
			for (var targetColumn : targetColumnList) {
				// 대상 컬럼에 대해 처리 작업을 해야 함.
				// 트리 구조에서 대상 컬럼 탐색 후 처리
				var plainSelect = (PlainSelect) CCJSqlParserUtil.parse(sqlStr);
				
				
				// 우선 컬럼 제거 부터 테스트 해보자.

				List<String> whereColumnRightExpressionTypeList = findWhereClauseRightExpressionContainsTypeList(plainSelect, targetColumn);
				
				StringBuilder builder = new StringBuilder();
				StatementDeParser deParser = new StatementDeParser(builder);
				deParser.visit(plainSelect);
				
				
				
				log.debug("Query Result : {}", builder.toString());
				log.debug("target Column : {}", targetColumn);
				log.debug("target Column rightExpression : {}", whereColumnRightExpressionTypeList);
			}
			
		}
	}
	
	
	private List<String> findWhereClauseRightExpressionContainsTypeList(PlainSelect plainSelect, String columnName) {
		var rightExpression = findWhereClauseRightExpression(plainSelect, columnName);
		
		if (rightExpression == null) {
			return Collections.emptyList();
		}
		
		var expressionContainsTypeList = new ArrayList<String>();
		
		rightExpression.accept(new ExpressionVisitorAdapter() {
		    @Override
		    public void visit(JdbcNamedParameter parameter) {
		    	expressionContainsTypeList.add(parameter.getClass().getSimpleName());
		    }
		    
		    
		    @Override
		    public void visit(StringValue value) {
		    	expressionContainsTypeList.add(value.getClass().getSimpleName());
		    }
		    
		    @Override
		    public void visit(LongValue value) {
		    	expressionContainsTypeList.add(value.getClass().getSimpleName());
		    }
		});
		
		return expressionContainsTypeList;
	}
	
	
	// parser 처리 케이스 구현
	
	// 기본 흐름
	// pageRequest를 전달받았다고 간주하고
	// 대상 쿼리를 기준으로 count 쿼리 생성
	// limit offset 처리된 쿼리 생성
	
	// 여기서 이거저거 조건들이 붙은 경우 처리
	
	
	
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
}
