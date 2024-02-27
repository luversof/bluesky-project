package net.luversof.web.dynamiccrud.setting.util;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import net.luversof.web.dynamiccrud.setting.domain.DbField;
import net.luversof.web.dynamiccrud.setting.domain.DbFieldSearchType;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;

@Slf4j
@UtilityClass
public class JSqlParserUtil {

	private static String PERCENTAGE = "%";
	
	/**
	 * DbField의 설정을 기준으로 추가할 Where 절 Expression 계산
	 * @param dbFieldSearchType
	 * @return
	 */
	public static Expression createWhereClauseAppendExpression(DbField dbField) {
		return switch (dbField.getColumnSearchType()) {
		case LIKE_RIGHT -> {
			var likeExpression = new LikeExpression();
			likeExpression.setLeftExpression(new Column(dbField.getColumnId()));
			likeExpression.setLikeKeyWord(DbFieldSearchType.convertJsqlKeyword(dbField.getColumnSearchType()));
			Addition addExpression = new Addition();
			addExpression.withLeftExpression(new JdbcNamedParameter(dbField.getColumnId()));
			addExpression.withRightExpression(new StringValue(PERCENTAGE));
			likeExpression.setRightExpression(addExpression);
			log.debug("likeExpression, {}",likeExpression);
			yield likeExpression;
		}
		case LIKE_CONTAINS -> {
			var likeContainsExpression = new LikeExpression();
			likeContainsExpression.setLeftExpression(new Column(dbField.getColumnId()));
			likeContainsExpression.setLikeKeyWord(DbFieldSearchType.convertJsqlKeyword(dbField.getColumnSearchType()));
			Addition addExpressionTarget = new Addition();
			addExpressionTarget.withLeftExpression(new StringValue(PERCENTAGE));
			addExpressionTarget.withRightExpression(new JdbcNamedParameter(dbField.getColumnId()));
			Addition addExpressionTarget2 = new Addition();
			addExpressionTarget2.withLeftExpression(addExpressionTarget);
			addExpressionTarget2.withRightExpression(new StringValue(PERCENTAGE));
			likeContainsExpression.setRightExpression(addExpressionTarget2);
			log.debug("likeExpression contains, {}",likeContainsExpression);
			yield likeContainsExpression;
		}
		case MINOR_THAN -> {
			var minorExpression = new MinorThan();
			minorExpression.setLeftExpression(new Column(dbField.getColumnId()));
			minorExpression.setRightExpression(new JdbcNamedParameter(dbField.getColumnId()));
			log.debug("minorExpression, {}",minorExpression);
			yield minorExpression;
		}
		case GREATER_THAN -> {
			var greaterExpression = new GreaterThan();
			greaterExpression.setLeftExpression(new Column(dbField.getColumnId()));
			greaterExpression.setRightExpression(new JdbcNamedParameter(dbField.getColumnId()));
			log.debug("greaterExpression, {}",greaterExpression);
			yield greaterExpression;
		}
		default -> new EqualsTo(new Column(dbField.getColumnId()), new JdbcNamedParameter(dbField.getColumnId()));
		};
	}
	
	/**
	 * Where 절에서 해당 컬럼명으로 지정된 타입 검색
	 * @param plainSelect
	 * @param columnName
	 * @return
	 */
	public static List<String> findWhereClauseRightExpressionContainsTypeList(PlainSelect plainSelect, String columnName) {
		var rightExpression = findWhereClauseRightExpression(plainSelect, columnName);
		
		if (rightExpression == null) {
			return Collections.emptyList();
		}
		
		var expressionContainsTypeList = new ArrayList<String>();
		
		rightExpression.accept(new ExpressionVisitorAdapter() {
		    
			@Override
		    public void visit(JdbcNamedParameter parameter) {
		    	expressionContainsTypeList.add("jdbcNamedParameter");
		    }
		    
		    
		    @Override
		    public void visit(StringValue value) {
		    	expressionContainsTypeList.add("StringValue");
		    }
		    
		    @Override
		    public void visit(LongValue value) {
		    	expressionContainsTypeList.add("LongValue");
		    }
		
		});
		
		return expressionContainsTypeList;
	}
	
	/**
	 * Where 절에서 해당 컬럼에 대한 rightExpression 검색
	 * @param plainSelect
	 * @param columnName
	 * @return
	 */
	public static Expression findWhereClauseRightExpression(PlainSelect plainSelect, String columnName) {
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
	
	public static Expression findWhereClauseRightExpressionNested(BinaryExpression superExpression, String columnName) {
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
	
	
	/**
	 * where 절에서 leftExpression Column이 columnName인 경우 제거
	 * @param plainSelect
	 * @param columnName
	 */
	public static void removeWhereClauseByColumnName(PlainSelect plainSelect, String columnName) {
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
	
	
	public static void removeWhereClauseByColumnNameNested(BinaryExpression superExpression, String columnName) {
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
	}
}
