package net.luversof.api.board.controller;

import java.util.List;
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

import net.luversof.api.board.domain.BoardArticleComment;
import net.luversof.api.board.domain.BoardArticleCommentCount;
import net.luversof.api.board.service.BoardArticleCommentService;

/** 게시글 댓글 */
@RestController
@RequestMapping(value = "/api/boardArticleComment", produces = MediaType.APPLICATION_JSON_VALUE)
public class BoardArticleCommentController {

  private BoardArticleCommentService boardArticleCommentService;

  @Autowired
  public void setBoardArticleCommentService(BoardArticleCommentService boardArticleCommentService) {
    this.boardArticleCommentService = boardArticleCommentService;
  }

  /**
   * 게시글 댓글 작성
   *
   * @param boardArticleComment 쓸 댓글(게시글 id, 작성자 userId, 내용)
   * @return 저장한 댓글
   */
  @PostMapping
  public BoardArticleComment create(
      @Validated(BoardArticleComment.Create.class) @RequestBody
          BoardArticleComment boardArticleComment) {
    return boardArticleCommentService.save(boardArticleComment);
  }

  /**
   * 게시글의 댓글 목록 조회
   *
   * <p>기본은 한 쪽 10 건, 오래된 댓글부터.
   *
   * @param boardArticleId 게시글 ID
   * @param pageable 쪽 · 크기 · 정렬
   * @return 댓글 한 쪽
   */
  @GetMapping("/search/findByBoardArticleId/{boardArticleId}")
  public Page<BoardArticleComment> findByBoardArticleId(
      @PathVariable UUID boardArticleId,
      @PageableDefault(size = 10) @SortDefault(sort = "createdDate", direction = Direction.ASC)
          Pageable pageable) {
    return boardArticleCommentService.findByBoardArticleId(boardArticleId, pageable);
  }

  /**
   * 게시글의 댓글 수 조회
   *
   * @param boardArticleId 게시글 ID
   * @return 댓글 수
   */
  @GetMapping("/search/countByBoardArticleId/{boardArticleId}")
  public long countByBoardArticleId(@PathVariable UUID boardArticleId) {
    return boardArticleCommentService.countByBoardArticleId(boardArticleId);
  }

  /**
   * 여러 게시글의 댓글 수 조회
   *
   * <p>목록 화면용. 조회지만 게시글 ID 목록을 본문으로 받으려고 POST 다.
   *
   * @param boardArticleIds 게시글 ID 목록
   * @return 게시글별 댓글 수(댓글이 없는 글은 빠진다)
   */
  @PostMapping("/search/countByBoardArticleIds")
  public List<BoardArticleCommentCount> countByBoardArticleIds(
      @RequestBody List<UUID> boardArticleIds) {
    return boardArticleCommentService.countByBoardArticleIds(boardArticleIds);
  }

  /**
   * 게시글 댓글 수정
   *
   * <p>작성자 본인만 고칠 수 있다.
   *
   * @param boardArticleComment 고칠 댓글(id, 요청한 사용자 userId 필수)
   * @return 고친 댓글
   */
  @PutMapping
  public BoardArticleComment modify(
      @Validated(BoardArticleComment.Modify.class) @RequestBody
          BoardArticleComment boardArticleComment) {
    return boardArticleCommentService.update(boardArticleComment);
  }

  /**
   * 게시글 댓글 삭제
   *
   * <p>작성자 본인만 지울 수 있다.
   *
   * @param boardArticleComment 지울 댓글(id, 요청한 사용자 userId 필수)
   */
  @DeleteMapping
  public void delete(
      @Validated(BoardArticleComment.Delete.class) @RequestBody
          BoardArticleComment boardArticleComment) {
    boardArticleCommentService.delete(boardArticleComment);
  }
}
