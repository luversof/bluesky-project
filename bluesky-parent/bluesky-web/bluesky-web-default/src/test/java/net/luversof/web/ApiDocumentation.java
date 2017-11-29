package net.luversof.web;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.linkWithRel;
import static org.springframework.restdocs.hypermedia.HypermediaDocumentation.links;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import net.luversof.GeneralTest;



public class ApiDocumentation extends GeneralTest {
	
	@Rule
	public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();
	
	private MockMvc mockMvc;

	@Autowired
	private WebApplicationContext context;

	@Before
	public void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.apply(documentationConfiguration(this.restDocumentation)) 
				.build();
	}
	
	@Test
	public void test() {
		
	}
	
	@Test
	public void indexExample() throws Exception {
		this.mockMvc.perform(get("/api/"))
			.andExpect(status().isOk())
			.andDo(document("index-example",
					links(
							linkWithRel("entryGroups").description("The <<resources-entryGroups,EntryGroups resource>>"),
							linkWithRel("blogs").description("The <<resources-blogs,Blogs resource>>"),
							linkWithRel("blogArticles").description("The <<resources-blogArticles,BlogArticles resource>>"),
							linkWithRel("users").description("The <<resources-users,users resource>>"),
							linkWithRel("bbses").description("The <<resources-bbses,bbses resource>>"),
							linkWithRel("entries").description("The <<resources-entries,entries resource>>"),
							linkWithRel("userAuthorities").description("The <<resources-userAuthorities,userAuthorities resource>>"),
							linkWithRel("categories").description("The <<resources-categories,categories resource>>"),
							linkWithRel("articles").description("The <<resources-articles,articles resource>>"),
							linkWithRel("bookkeepings").description("The <<resources-bookkeepings,Bookkeepings resource>>"),
							linkWithRel("assets").description("The <<resources-assets,Assets resource>>"),
							linkWithRel("profile").description("The ALPS profile for the service")),
					responseFields(
							subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"))));

	}

}
