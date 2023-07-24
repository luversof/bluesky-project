package net.luversof.api.board.controller.swagger;

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
import net.luversof.api.board.domain.Board;

public @interface BoardControllerOperation {

	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@Operation(
		tags = "Board",
		summary = "Board 저장",
		description = """
				Board 저장 설명 
			""",
		parameters = @Parameter(
			name = "blog",
			schema = @Schema(implementation = Board.class),
			examples = {
				@ExampleObject(
					name = "board 저장 요청 예제1",
					description = """
						예제 1 설명
					""",
					value = """
						{
						  "alias": "testAlias"
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
						schema = @Schema(implementation = Board.class),
						examples = {
							@ExampleObject(
								name = "Board 저장 응답 예제 1",
								description = """
									예제 1 설명
								""",
								value = """
									{
									  "idx": 0,
									  "alias": "testAlias"
									}
								"""
							)
						}
					)
				}
			)
		}
	)
	@ApiResponse401
	@interface Create {
		
	}

	@Target({ElementType.METHOD, ElementType.TYPE, ElementType.ANNOTATION_TYPE})
	@Retention(RetentionPolicy.RUNTIME)
	@Inherited
	@ApiResponse(
			responseCode = "401",
			content = {
				@Content(
					schema = @Schema(implementation = Board.class),
					examples = {
						@ExampleObject(
							name = "Board 저장 응답 401 예제",
							description = """
								예제 1 설명
							""",
							value = """
								{
								  "status": 401,
								  "alias": "testAlias"
								}
							"""
						)
					}
				)
			}
		)
	@interface ApiResponse401 {}

}
