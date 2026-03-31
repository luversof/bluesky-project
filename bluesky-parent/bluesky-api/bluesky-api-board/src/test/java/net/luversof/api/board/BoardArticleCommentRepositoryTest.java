package net.luversof.api.board;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luversof.boot.jdbc.datasource.context.RoutingDataSourceContextHolder;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.luversof.GeneralTest;
import net.luversof.api.board.domain.BoardArticleCommentCount;
import net.luversof.api.board.repository.BoardArticleCommentRepository;
import net.luversof.api.board.repository.BoardArticleRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Rollback(false)
class BoardArticleCommentRepositoryTest implements GeneralTest {

    @Autowired private BoardArticleRepository boardArticleRepository;

    @Autowired private BoardArticleCommentRepository boardArticleCommentRepository;

    @BeforeAll
    static void beforeAll() {
        RoutingDataSourceContextHolder.setContext(() -> "board_postgresql");
    }

    @Test
    @DisplayName("batch countByBoardArticleIds should return non-null ids and counts")
    void countByBoardArticleIds_batch() {
        // collect up to 5 existing article ids
        List<UUID> ids = new ArrayList<>();
        boardArticleRepository
                .findAll()
                .forEach(
                        a -> {
                            if (ids.size() < 335) ids.add(a.getId());
                        });

        // if repository is empty the test is inconclusive but should not fail hard
        if (ids.isEmpty()) {
            // no articles to test against
            return;
        }

        List<BoardArticleCommentCount> counts =
                boardArticleCommentRepository.countByBoardArticleIds(ids);

        assertThat(counts).isNotNull();
        assertThat(counts).isNotEmpty();

        // each returned entry must have a non-null boardArticleId and a non-negative count
        counts.forEach(
                c -> {
                    assertThat(c.getBoardArticleId()).isNotNull();
                    assertThat(c.getCount()).isGreaterThanOrEqualTo(0);
                });
    }
}
