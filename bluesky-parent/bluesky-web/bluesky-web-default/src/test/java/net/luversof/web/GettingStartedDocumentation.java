package net.luversof.web;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.MediaTypes;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import net.luversof.GeneralTest;

public class GettingStartedDocumentation extends GeneralTest {

	@Rule
	public final JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation();

//	@Autowired
//	private ObjectMapper objectMapper;

	@Autowired
	private WebApplicationContext context;

	private MockMvc mockMvc;

	@Before
	public void setUp() {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(this.context)
				.apply(documentationConfiguration(this.restDocumentation))
				.alwaysDo(document("api/{method-name}/{step}/"))
				.build();
	}

	@Test
	public void index() throws Exception {
		this.mockMvc.perform(get("/api").accept(MediaTypes.HAL_JSON))
			.andExpect(status().isOk());
			//.andExpect(jsonPath("_links.notes", is(notNullValue())))
			//.andExpect(jsonPath("_links.tags", is(notNullValue())));
	}


}
