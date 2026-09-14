package net.luversof.web.common.menu;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import net.luversof.web.common.menu.domain.Pagination;

/**
 * The page block (ten numbers at a time) and the four arrows around it.
 *
 * <p>Measured 2026-09-12 on the trade history list (258 rows; size=24 makes 11 pages): the pager
 * printed 1-10 and then disabled BOTH the next and the last arrow, so page 11 could not be reached
 * from the pager at all. The next page was computed with endPage < totalPage - 1, which asks
 * whether a page after the next one exists. With 12 or 13 pages it worked, which is why it stayed
 * hidden for so long.
 */
class PaginationTest {

  private Pagination pagination(int totalItems, int pageSize, int zeroBasedPage) {
    return new Pagination(
        new PageImpl<>(List.of(), PageRequest.of(zeroBasedPage, pageSize), totalItems));
  }

  private int lastNavPage(Pagination pagination) {
    return pagination.getNavList().get(pagination.getNavList().size() - 1).page();
  }

  /** One page past the block must stay reachable - this is the case that was broken. */
  @Test
  void 열한_쪽이면_다음과_마지막이_살아_있다() {
    Pagination pagination = pagination(258, 24, 0);
    assertThat(pagination.getNavList()).hasSize(10);
    assertThat(lastNavPage(pagination)).isEqualTo(10);
    assertThat(pagination.getNextNav().isActive()).as("next arrow").isTrue();
    assertThat(pagination.getNextNav().page()).as("first page of the next block").isEqualTo(11);
    assertThat(pagination.getLastNav().isActive()).as("last arrow").isTrue();
    assertThat(pagination.getLastNav().page()).isEqualTo(11);
  }

  /** Every block boundary, not just the eleventh page. */
  @Test
  void 묶음_경계마다_남은_쪽이_있으면_살아_있다() {
    for (int totalPages = 2; totalPages <= 45; totalPages++) {
      Pagination pagination = pagination(totalPages * 10, 10, 0);
      boolean beyondBlock = totalPages > 10;
      assertThat(pagination.getNextNav().isActive())
          .as("total " + totalPages + " pages, next arrow")
          .isEqualTo(beyondBlock);
      assertThat(pagination.getLastNav().isActive())
          .as("total " + totalPages + " pages, last arrow")
          .isEqualTo(beyondBlock);
      if (beyondBlock) {
        assertThat(pagination.getNextNav().page())
            .as("total " + totalPages + " pages, next target")
            .isEqualTo(11);
        assertThat(pagination.getLastNav().page()).isEqualTo(totalPages);
      }
    }
  }

  /** The last block has nothing after it, and the first block nothing before it. */
  @Test
  void 끝_묶음에서는_다음이_없고_첫_묶음에서는_이전이_없다() {
    Pagination first = pagination(258, 24, 0);
    assertThat(first.getPrevNav().isActive()).isFalse();
    assertThat(first.getFirstNav().isActive()).isFalse();

    Pagination last = pagination(258, 24, 10);
    assertThat(last.getNavList()).hasSize(1);
    assertThat(lastNavPage(last)).isEqualTo(11);
    assertThat(last.getNextNav().isActive()).isFalse();
    assertThat(last.getLastNav().isActive()).isFalse();
    assertThat(last.getPrevNav().isActive()).isTrue();
    assertThat(last.getPrevNav().page()).as("last page of the previous block").isEqualTo(10);
    assertThat(last.getFirstNav().isActive()).isTrue();
    assertThat(last.getFirstNav().page()).isEqualTo(1);
  }

  /** A single block needs no arrows at all. */
  @Test
  void 한_묶음이면_화살표가_모두_꺼진다() {
    Pagination pagination = pagination(258, 26, 0);
    assertThat(pagination.getNavList()).hasSize(10);
    assertThat(pagination.getNextNav().isActive()).isFalse();
    assertThat(pagination.getLastNav().isActive()).isFalse();
    assertThat(pagination.getPrevNav().isActive()).isFalse();
    assertThat(pagination.getFirstNav().isActive()).isFalse();
  }
}
