package net.luversof.web.gate.board.httpexchange;

import io.github.luversof.boot.data.domain.PageResponse;
import java.util.Optional;
import java.util.UUID;
import net.luversof.web.gate.board.domain.BoardArticle;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

/** 게시글 관련 API를 호출하는 HttpExchange 클라이언트 (bluesky-api-board 호출) */
@HttpExchange(url = "/api/boardArticle", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface BoardArticleClient {

    /** 새 게시글 작성 */
    @PostExchange
    BoardArticle create(@RequestBody BoardArticle boardArticle);

    /** 게시판 alias로 게시글 목록 조회 (페이지네이션) */
    @GetExchange("/search/findByBoardAlias/{boardAlias}")
    PageResponse<BoardArticle> findByBoardAlias(@PathVariable String boardAlias, Pageable pageable);

    /** ID로 특정 게시글 조회 */
    @GetExchange("/{id}")
    Optional<BoardArticle> findById(@PathVariable UUID id);

    /** 게시글 수정 */
    @PutExchange
    BoardArticle modify(@RequestBody BoardArticle boardArticle);

    /** 게시글 삭제 */
    @DeleteExchange
    void delete(@RequestBody BoardArticle boardArticle);
}
