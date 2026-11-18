package net.luversof.web.dynamiccrud2.admin.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import net.luversof.web.dynamiccrud2.adapter.TableMeta;
import net.luversof.web.dynamiccrud2.adapter.TableMeta.IndexInfo;
import net.luversof.web.dynamiccrud2.adapter.TableRef;
import net.luversof.web.dynamiccrud2.domain.SearchOperator;

/** 검색 방식마다 인덱스를 쓰는지 - 실행 쿼리 모양(LOWER LIKE · = · 범위)과 인덱스 첫 컬럼 규칙 */
class SearchIndexUseTest {

  private static final TableMeta META =
      new TableMeta(
          new TableRef(null, "public", "t"),
          List.of(),
          List.of("id"),
          List.of(),
          Set.of("id", "a"),
          List.of(
              new IndexInfo("t_pkey", List.of("id"), true, true, false, false),
              new IndexInfo("idx_a_b", List.of("a", "b"), false, false, false, false),
              new IndexInfo("idx_lower_c", List.of("lower(c)"), false, false, false, true)));

  @Test
  void equalsAndRangeUseIndexOnlyOnLeadingColumn() {
    assertThat(SearchService.indexUse(META, "id", SearchOperator.EQUALS).ok()).isTrue();
    assertThat(SearchService.indexUse(META, "a", SearchOperator.RANGE).text()).contains("idx_a_b");
    // 복합 인덱스의 두 번째 컬럼은 단독으로 못 쓴다
    var second = SearchService.indexUse(META, "b", SearchOperator.EQUALS);
    assertThat(second.ok()).isFalse();
    assertThat(second.text()).contains("첫 컬럼이 아니라");
    // 식 인덱스는 컬럼 검색에 쓰지 않는다
    assertThat(SearchService.indexUse(META, "c", SearchOperator.EQUALS).ok()).isFalse();
  }

  @Test
  void likeOperatorsNeverUseIndexAndMissingMetaIsNotOk() {
    assertThat(SearchService.indexUse(META, "id", SearchOperator.CONTAINS).ok()).isFalse();
    assertThat(SearchService.indexUse(META, "a", SearchOperator.STARTS_WITH).ok()).isFalse();
    assertThat(SearchService.indexUse(null, "a", SearchOperator.EQUALS).ok()).isFalse();
  }
}
