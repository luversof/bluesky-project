package net.luversof.api.board.controller;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.board.domain.BoardArticle;
import net.luversof.api.board.service.BoardArticleService;

/** 게시글. */
@RestController
@RequestMapping(value = "/api/boardArticle", produces = MediaType.APPLICATION_JSON_VALUE)
public class BoardArticleController {

  private BoardArticleService boardArticleService;

  @Autowired
  public void setBoardArticleService(BoardArticleService boardArticleService) {
    this.boardArticleService = boardArticleService;
  }

  /**
   * 게시글을 쓴다.
   *
   * @param boardArticle 쓸 글(게시판 alias, 작성자 userId, 제목, 내용)
   * @return 저장한 글
   */
  @PostMapping
  public BoardArticle create(
      @Validated(BoardArticle.Create.class) @RequestBody BoardArticle boardArticle) {
    return boardArticleService.save(boardArticle);
  }

  /**
   * 게시판의 글 목록. 기본은 한 쪽 20 건, 최신 글부터.
   *
   * @param boardAlias 게시판 alias
   * @param pageable 쪽 · 크기 · 정렬
   * @return 글 한 쪽
   */
  @GetMapping("/search/findByBoardAlias/{boardAlias}")
  public Page<BoardArticle> findByBoardAlias(
      @PathVariable String boardAlias,
      @PageableDefault(size = 20) @SortDefault(sort = "createdDate", direction = Direction.DESC)
          Pageable pageable) {
    return boardArticleService.findByAlias(boardAlias, pageable);
  }

  /**
   * 게시글 한 건.
   *
   * @param id 게시글 ID
   * @return 게시글, 없으면 빈 값
   */
  @GetMapping("/{id}")
  public Optional<BoardArticle> findByBoardArticleId(@PathVariable UUID id) {
    return boardArticleService.findById(id);
  }

  /**
   * 게시글을 고친다. 작성자 본인만 고칠 수 있다.
   *
   * @param boardArticle 고칠 글(id, 요청한 사용자 userId 필수)
   * @return 고친 글
   */
  @PutMapping
  public BoardArticle modify(
      @Validated(BoardArticle.Modify.class) @RequestBody BoardArticle boardArticle) {
    return boardArticleService.update(boardArticle);
  }

  /**
   * 게시글을 지운다. 작성자 본인만 지울 수 있고, 글의 댓글도 함께 지운다.
   *
   * @param boardArticle 지울 글(id, 요청한 사용자 userId 필수)
   */
  @DeleteMapping
  public void delete(@Validated(BoardArticle.Delete.class) @RequestBody BoardArticle boardArticle) {
    boardArticleService.delete(boardArticle);
  }
}
