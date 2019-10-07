package net.luversof.web;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.MediaTypes;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import net.luversof.GeneralTest;

public class GettingStartedDocumentation extends GeneralTest {

//	@Autowired
//	private ObjectMapper objectMapper;

	private MockMvc mockMvc;

	@BeforeEach
	public void setUp(WebApplicationContext webApplicationContext,
			RestDocumentationContextProvider restDocumentation) {
		this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
				.apply(documentationConfiguration(restDocumentation))
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
