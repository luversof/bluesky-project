//package net.luversof.web.doc;
//
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.BDDMockito.given;
//import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
//import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
//import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
//import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
//import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
//import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
//import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
//import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//import java.util.HashMap;
//
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.http.MediaType;
//import org.springframework.restdocs.payload.FieldDescriptor;
//import org.springframework.restdocs.payload.JsonFieldType;
//import org.springframework.test.context.ActiveProfiles;
//import org.springframework.test.context.ContextConfiguration;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import io.github.luversof.boot.test.autoconfigure.restdocs.RestDocsTest;
//import lombok.SneakyThrows;
//import net.luversof.TestApplication;
//import net.luversof.blog.domain.mysql.BlogArticle;
//import net.luversof.web.blog.controller.BlogArticleController;
//
////@AutoConfigureRestDocs
////@SpringBootTest(classes = TestApplication.class)
//
//@WebMvcTest(BlogArticleController.class)
//@ContextConfiguration(classes = { TestApplication.class })
//@ActiveProfiles("localdev")
//public class BlogArticleTest extends RestDocsTest {
//
//	@Autowired
//	private ObjectMapper objectMapper;
//
//	@MockBean
//	private BlogArticleController blogArticleController;
//	
//	private static FieldDescriptor[] blogFields = new FieldDescriptor[] {
//			fieldWithPath("id").type(JsonFieldType.STRING).description("blog Id"),
//			fieldWithPath("userId").type(JsonFieldType.STRING).description("유저 Id"),
//			fieldWithPath("createdDate").type(JsonFieldType.STRING).description("유저 가입일") };
//
//	private static FieldDescriptor[] blogArticleFields = new FieldDescriptor[] {
//			fieldWithPath("id").type(JsonFieldType.NUMBER).description("blogArticle Id"),
//			fieldWithPath("title").type(JsonFieldType.STRING).description("제목"),
//			fieldWithPath("content").type(JsonFieldType.STRING).description("내용"),
//			fieldWithPath("createdDate").type(JsonFieldType.STRING).description("생성일"),
//			fieldWithPath("lastModifiedDate").type(JsonFieldType.STRING).description("수정일"),
//			fieldWithPath("userId").type(JsonFieldType.STRING).description("유저 Id"),
//			fieldWithPath("viewCount").type(JsonFieldType.NUMBER).description("조회수"),
//			fieldWithPath("blogCommentCount").type(JsonFieldType.NUMBER).description("댓글수"),
//			fieldWithPath("blogArticleCategory").type(JsonFieldType.OBJECT).optional().description("blogArticle category 정보 참조") };
//
//	@BeforeAll
//	static void beforeAll() {
//		
//	}
//
//	@Test
//	@SneakyThrows
//	public void findById() {
//		given(blogArticleController.findByBlogArticleId(anyString()))
//				.willReturn(getMockOptional("blogArticle/blogArticle.json", BlogArticle.class));
//
//		this.mockMvc.perform(get("/api/blogArticle/{id}", 1).contentType(MediaType.APPLICATION_JSON))
//				.andExpect(status().isOk())
//				.andDo(document("blogArticle/findById",
//						pathParameters(parameterWithName("id").description("blogArticle Id")),
//						responseFields(blogArticleFields).andWithPrefix("blog.", blogFields)));
//	}
//
//	@Test
//	@SneakyThrows
//	public void create() {
//		given(blogArticleController.create(any()))
//				.willReturn(getMock("blogArticle/blogArticle.json", BlogArticle.class));
//
//		var blogArticle = new HashMap<>();
//		blogArticle.put("title", "title");
//		blogArticle.put("content", "content");
//		
//		this.mockMvc.perform(post("/api/blogArticle").contentType(MediaType.APPLICATION_JSON)
//				.content(objectMapper.writeValueAsString(blogArticle))
//				)
//				.andExpect(status().isOk())
//				.andDo(document("blogArticle/create",
//						pathParameters(),
//						requestFields(
//								fieldWithPath("title").description("title"),
//								fieldWithPath("content").description("content")),
//						responseFields(blogArticleFields).andWithPrefix("blog.", blogFields)));
//	}
//}
