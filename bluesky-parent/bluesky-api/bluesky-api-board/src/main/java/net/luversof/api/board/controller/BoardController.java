package net.luversof.api.board.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.board.domain.Board;
import net.luversof.api.board.service.BoardService;

/**
 * 게시판
 *
 * <p>alias 로 구분한다.
 */
@RestController
@RequestMapping(value = "/api/board", produces = MediaType.APPLICATION_JSON_VALUE)
public class BoardController {

  private BoardService boardService;

  @Autowired
  public void setBoardService(BoardService boardService) {
    this.boardService = boardService;
  }

  /**
   * 게시판 생성
   *
   * @param board 만들 게시판(alias 필수, 게시판 주소에 쓰인다)
   * @return 만든 게시판
   */
  @PostMapping
  public Board create(@RequestBody Board board) {
    return boardService.create(board);
  }

  /**
   * alias로 게시판 조회
   *
   * @param alias 게시판 alias
   * @return 게시판(없으면 오류)
   */
  @GetMapping("/search/findByAlias/{alias}")
  public Board findByAlias(@PathVariable String alias) {
    return boardService.findByAlias(alias);
  }

  /**
   * 게시판 목록 조회
   *
   * @return 게시판 목록
   */
  @GetMapping("/search/findAll")
  public Iterable<Board> findAll() {
    return boardService.findAll();
  }

  /**
   * 게시판 수정
   *
   * @param board 고칠 게시판
   * @return 고친 게시판
   */
  @PutMapping
  public Board update(@RequestBody Board board) {
    return boardService.update(board);
  }
}
