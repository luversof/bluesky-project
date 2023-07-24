package net.luversof.api.blog.controller.swagger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import net.luversof.api.blog.domain.mariadb.Blog;

public @interface BlogControllerOperation {

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Operation(
		tags = "Blog",
		summary = "Blog 저장",
		description = """
				Blog 저장 설명 
			""",
		parameters = @Parameter(
			name = "blog",
			schema = @Schema(implementation = Blog.class),
			examples = {
				@ExampleObject(
					name = "blog 저장 요청 예제1",
					description = """
						예제 1 설명
					""",
					value = """
						{
						  "userId": "testUserId"
						}
					"""
				)
			}
		),
		responses = {
			@ApiResponse(
				responseCode = "200",
				content = {
					@Content(
						schema = @Schema(implementation = Blog.class),
						examples = {
							@ExampleObject(
								name = "blog 저장 응답 예제 1",
								description = """
									예제 1 설명
								""",
								value = """
									{
									  "idx": 0,
									  "userId": "CBB5D7CA-5A48-E011-8195-18A90577F94A",
									  "blogId": "E50D9C4B-050C-4DCC-B331-B6F095661E9D"
									}
								"""
							)
						}
					)
				}
			)
		}
	)
	@interface Create {
		
	}
}
