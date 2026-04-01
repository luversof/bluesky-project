package net.luversof.web.gate.board.httpexchange;

import io.github.luversof.boot.data.domain.PageResponse;
import java.util.List;
import java.util.UUID;
import net.luversof.web.gate.board.domain.BoardArticleComment;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import org.springframework.web.service.annotation.PutExchange;

@HttpExchange(url = "/api/boardArticleComment", contentType = MediaType.APPLICATION_JSON_VALUE)
public interface BoardArticleCommentClient {

    @PostExchange
    BoardArticleComment create(@RequestBody BoardArticleComment boardArticleComment);

    @GetExchange("/search/findByBoardArticleId/{boardArticleId}")
    PageResponse<BoardArticleComment> findByBoardArticleId(
            @PathVariable UUID boardArticleId, Pageable pageable);

    @GetExchange("/search/countByBoardArticleId/{boardArticleId}")
    long countByBoardArticleId(@PathVariable UUID boardArticleId);

    @PostExchange("/search/countByBoardArticleIds")
    List<net.luversof.web.gate.board.domain.BoardArticleCommentCount> countByBoardArticleIds(
            @RequestBody List<UUID> boardArticleIds);

    @PutExchange
    BoardArticleComment modify(@RequestBody BoardArticleComment boardArticleComment);

    @DeleteExchange
    void delete(@RequestBody BoardArticleComment boardArticleComment);
}
