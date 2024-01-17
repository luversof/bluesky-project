package net.luversof.web.dynamiccrud.use;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;

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
import net.luversof.web.dynamiccrud.use.domain.ContentInfo;
import net.luversof.web.dynamiccrud.use.service.UseServiceDecorator;
import net.luversof.web.dynamiccrud.use.util.ThymeleafUseUtil;

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

		var query = ThymeleafUseUtil.getDbQuery(adminProjectId, projectId, mainMenuId, subMenuId, DbQuerySqlCommandType.SELECT);
		assertThat(query).isNotNull();

		var fieldList = ThymeleafUseUtil.getDbFieldList(adminProjectId, projectId, mainMenuId, subMenuId);
		assertThat(fieldList).isNotEmpty();

		var pageable = PageRequest.of(0, 20);

		var paramMap = Collections.<String, String>emptyMap();

		var page = useService.find(query, fieldList, pageable, paramMap);

		var contentInfo = new ContentInfo(page.getContent(), fieldList);
		assertThat(contentInfo).isNotNull();

		log.debug("contentInfo processedContentMapList : {}",
				objectMapper.writeValueAsString(contentInfo.getProcessedContentMapList()));

//		useService.find(null, null, null, null)
	}

	@Test
	void messageFormatTest() {
		
		var expressionParser = new SpelExpressionParser();

		var evaluationContext = new StandardEvaluationContext();
		evaluationContext.setVariable("testKey", "testValue");
		
		Expression expression = expressionParser.parseExpression("'Any string' + #testKey");
		String result = (String) expression.getValue(evaluationContext);
		
		log.debug("test : {}", result);
	}
}
