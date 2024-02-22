package net.luversof.web.dynamiccrud.use;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.web.dynamiccrud.setting.domain.DbQuerySqlCommandType;
import net.luversof.web.dynamiccrud.setting.domain.SettingParameter;
import net.luversof.web.dynamiccrud.setting.util.SettingStringUtil;
import net.luversof.web.dynamiccrud.setting.util.SettingUtil;
import net.luversof.web.dynamiccrud.use.domain.ContentInfo;
import net.luversof.web.dynamiccrud.use.service.UseServiceDecorator;

@Slf4j
public class UseTest implements GeneralTest {

	@Autowired
	private UseServiceDecorator useService;

	@Autowired
	private ObjectMapper objectMapper;

	@Test
	void contentInfoTest() throws JsonProcessingException {

//		var product = "setting";
//		var mainMenu = "menu";
//		var subMenu = "field";

		var adminProjectId = "eventAdmin";
		var projectId = "aaaa";
		var mainMenuId = "amenu";
		var subMenuId = "asubmenu";
		var settingParameter = new SettingParameter(adminProjectId, projectId, mainMenuId, subMenuId);

		var query = SettingUtil.getDbQuery(adminProjectId, projectId, mainMenuId, subMenuId, DbQuerySqlCommandType.SELECT);
		assertThat(query).isNotNull();

		var fieldList = SettingUtil.getDbFieldList(adminProjectId, projectId, mainMenuId, subMenuId);
		assertThat(fieldList).isNotEmpty();

		var pageable = PageRequest.of(0, 20);

		var paramMap = Collections.<String, String>emptyMap();

		var page = useService.find(settingParameter, pageable, paramMap);

		var contentInfo = new ContentInfo(page.getContent(), fieldList);
		assertThat(contentInfo).isNotNull();

		log.debug("contentInfo processedContentMapList : {}",
				objectMapper.writeValueAsString(contentInfo.getProcessedContentMapList()));

//		useService.find(null, null, null, null)
	}

	@Test
	void customSpelTest() {
		
		var expressionParser = new SpelExpressionParser();

		var evaluationContext = new StandardEvaluationContext();
		evaluationContext.setVariable("testKey", "testValue");
		
		Expression expression = expressionParser.parseExpression("'Any string' + #testKey");
		String result = (String) expression.getValue(evaluationContext);
		
		log.debug("test : {}", result);
	}
	
	@Test
	void messageFormatTest() {
//		String sqlTemplate = "select *, ${nonExistValue} From ${table} ${whereClause}";
		String sqlTemplate = """
				select 
					*, 
					${nonExistValue} 
				From ${table} 
				${whereClause}
				""";
		var param = new HashMap<String, String>();
		param.put("table", "users");
		param.put("whereClause", "where a = :aa and b = :bb");
		String result = SettingStringUtil.format(sqlTemplate, param);
		System.out.println(result);
	}
	
//	@Test
//	void tableNameFromQueryTest() {
////		String sql = "SELECT * FROM TET WHERE A = A";
//		String sql2 = """
//				SELECT 
//					*
//				FROM 
//				
//				dbo.TET 
//				
//				WHERE A = A
//				""";
//		String sql = "SELECT * FROM table ORDER BY column1 ASC, column2 DESC LIMIT 10";
//        System.out.println(SettingStringUtil.getTableName(sql));
//	}
//	
//	@Test
//	void isCustomQueryTest() {
//		String sql = "SELECT * FROM TET where A = A";
//		System.out.println(SettingStringUtil.isCustomQuery(sql));
//	}
//	
//	@Test
//	void orderClauseFromQueryTest() {
////		String sql = "SELECT * FROM table ORDER BY column1 ASC, column2 DESC LIMIT 10";
////		String sql = "SELECT * FROM table LIMIT 10";
//		String sql = """
//				SELECT * FROM dbo.BongInCancelRequest
//				ORDER BY nYear ASC
//				""";
//		System.out.println("----");
//		System.out.println(SettingStringUtil.getOrderClause(sql));
//		System.out.println("----");
//	}
	
}
