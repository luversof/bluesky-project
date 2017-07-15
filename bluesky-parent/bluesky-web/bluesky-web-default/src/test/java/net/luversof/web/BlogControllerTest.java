package net.luversof.web;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;

@Slf4j
@WebAppConfiguration
public class BlogControllerTest extends GeneralTest {
	
	@Autowired
	private WebApplicationContext webApplicationContext;
	
	@Test
	public void test() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.standaloneSetup(webApplicationContext).build();
		ResultActions resultActions = mockMvc.perform(MockMvcRequestBuilders.get("/blog/745B5D33-E9A1-4398-B997-BC3D6B83BF8B.json"));//.andExpect(MockMvcResultMatchers.status().isOk());
		log.debug("result : {}", resultActions.andReturn());
	}
}
