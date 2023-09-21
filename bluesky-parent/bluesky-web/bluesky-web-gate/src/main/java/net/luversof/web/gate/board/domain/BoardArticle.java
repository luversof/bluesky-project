package net.luversof.web.gate.board.domain;

import java.time.ZonedDateTime;

import lombok.Data;


@Data
public class BoardArticle {

	private long id;
	private String boardArticleId;
	private String userId;
	private String boardId;
	private String title;
	private String content;
	private ZonedDateTime createdDate;
	private ZonedDateTime lastModifiedDate;

}
