package net.luversof.api.board.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/board", produces = MediaType.APPLICATION_JSON_VALUE)
public class BoardController {

	//게시판 생성
	//게시판 수정
	//게시판 삭제? (혹은 비활성화)
}
