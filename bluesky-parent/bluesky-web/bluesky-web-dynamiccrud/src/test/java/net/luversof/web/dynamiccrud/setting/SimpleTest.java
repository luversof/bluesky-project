package net.luversof.web.dynamiccrud.setting;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import lombok.extern.slf4j.Slf4j;
import net.luversof.web.dynamiccrud.setting.domain.SubMenuDbType;


@Slf4j
class SimpleTest {

	@Test
	void stringSplitTest() {
		String a = "select * from test";
		
		
		log.debug("result : {}", a.split("FROM").length);
		log.debug("result : {}", a.split("from").length);
		log.debug("result : {}", Pattern.compile(" FROM ", Pattern.CASE_INSENSITIVE).split(a).length);
		log.debug("result : {}", Pattern.compile(" fRom ", Pattern.CASE_INSENSITIVE).split(a).length);
	}
	
	
//	@Test
//	void jooqTest() {
//		DSLContext dslContext = DSL.using(SQLDialect.MARIADB);
//		
//		dslContext.selectFrom("SELECT * FROM TABLEAA");
//		
//		var result = dslContext
//			.select()
//			.from("TABLENAME")
//			.where("asdf = :ASdf")
//			.getQuery();
//			;
//		System.out.println("result ----");
//		System.out.println(result.getSQL());
//	}
	
	@Test
	void enumToPreset() {
		
		var a = String.join("|", Stream.of(SubMenuDbType.values()).map(SubMenuDbType::name).collect(Collectors.toList()));
		log.debug("Test : {}", a);
		System.out.println(a);
		
	}
	
	@Test
	void documentParseTest() {
		var query = Document.parse("{ find: 'some_table', limit: 10, skip: 3 }");
		log.debug("query : {}", query);
		query.put("filter", Document.parse("{a : 'asdfsdf'}"));
		log.debug("query : {}", query);
		var filter = query.get("filter", Document.class);
		filter.put("b", "3gsdsfg");
//		query.append("filter", Document.parse("{b : 'e14325'}"));
		log.debug("query : {}", query);
	}
	
}
