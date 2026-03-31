package net.luversof.web.gate.board.httpexchange;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

import net.luversof.web.gate.board.domain.Board;

/** 게시판 관련 API를 호출하는 HttpExchange 클라이언트 (bluesky-api-board 호출) */
@HttpExchange(url = "/api/board", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface BoardClient {

    /** 새 게시판 생성 */
    @PostExchange
    Board create(@RequestBody Board board);

    /** 게시판 alias로 조회 */
    @GetExchange("/search/findByAlias/{alias}")
    Board findByAlias(@PathVariable String alias);

    /** 모든 게시판 목록 조회 */
    @GetExchange("/search/findAll")
    Iterable<Board> findAll();

    /** 게시판 정보 수정 */
    @PutExchange
    Board update(@RequestBody Board board);
}
