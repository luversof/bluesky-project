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

import javax.servlet.RequestDispatcher;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
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
				.alwaysDo(document("api/"))
				.build();
	}
	
	@Test
	public void errorExample() throws Exception {
		this.mockMvc
				.perform(get("/error")
						.requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 400)
						.requestAttr(RequestDispatcher.ERROR_REQUEST_URI,
								"/notes")
						.requestAttr(RequestDispatcher.ERROR_MESSAGE,
								"The tag 'http://localhost:8080/tags/123' does not exist"))
				.andDo(print()).andExpect(status().isBadRequest())
				.andExpect(jsonPath("error", is("Bad Request")))
				.andExpect(jsonPath("timestamp", is(notNullValue())))
				.andExpect(jsonPath("status", is(400)))
				.andExpect(jsonPath("path", is(notNullValue())))
				.andDo(document("error-example",
						responseFields(
								fieldWithPath("error").description("The HTTP error that occurred, e.g. `Bad Request`"),
								fieldWithPath("message").description("A description of the cause of the error"),
								fieldWithPath("path").description("The path to which the request was made"),
								fieldWithPath("status").description("The HTTP status code, e.g. `400`"),
								fieldWithPath("timestamp").description("The time, in milliseconds, at which the error occurred"))));
	}
	
	@Test
	public void indexExample() throws Exception {
		this.mockMvc.perform(get("/").accept(MediaType.APPLICATION_JSON)) 
		.andExpect(status().isOk()) 
		.andDo(document("index")); 
	}
	
	@Test
	public void apiExample() throws Exception {
		this.mockMvc.perform(get("/api"))
			.andExpect(status().isOk())
			.andDo(document("index-example",
					links(
							linkWithRel("entryGroups").description("The <<resources-entryGroups,EntryGroups resource>>"),
							linkWithRel("blogs").description("The <<resources-blogs,Blogs resource>>"),
							linkWithRel("blogArticles").description("The <<resources-blogArticles,BlogArticles resource>>"),
							linkWithRel("users").description("The <<resources-users,Users resource>>"),
							linkWithRel("bbses").description("The <<resources-bbses,Bbses resource>>"),
							linkWithRel("entries").description("The <<resources-entries,Entries resource>>"),
							linkWithRel("userAuthorities").description("The <<resources-userAuthorities,UserAuthorities resource>>"),
							linkWithRel("categories").description("The <<resources-categories,Categories resource>>"),
							linkWithRel("articles").description("The <<resources-articles,Articles resource>>"),
							linkWithRel("bookkeepings").description("The <<resources-bookkeepings,Bookkeepings resource>>"),
							linkWithRel("assets").description("The <<resources-assets,Assets resource>>"),
							linkWithRel("profile").description("The ALPS profile for the service")),
					responseFields(
							subsectionWithPath("_links").description("<<resources-index-links,Links>> to other resources"))));

	}

}
