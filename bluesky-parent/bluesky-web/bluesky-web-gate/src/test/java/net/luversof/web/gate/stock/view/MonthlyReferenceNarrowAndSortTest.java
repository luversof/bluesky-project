package net.luversof.web.gate.stock.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * 관리 &gt; 월배당 기준 데이터 화면의 좁은 폭 · 정렬 상태(2026-09-21).
 *
 * <p>실측(narrow-plus-font-reflow.js, 320px/200%): 이 화면만 문서가 <b>246px 넘쳤다</b>(문서 566px). 진원지는 셋이었다.
 *
 * <ol>
 *   <li>카드 제목 안 배지 &mdash; 배지는 기본이 한 줄(nowrap)이라 종목명이 길면 422px 이 된다.
 *   <li>단추 &mdash; 단추도 한 줄이라 "지급 이력 일괄 저장" 이 280px 바닥을 만든다.
 *   <li>입력칸 &mdash; {@code size=20} 만큼의 내재 폭이 있어 {@code w-full} 로는 안 줄어든다({@code min-w-0} 가 필요).
 * </ol>
 *
 * <p>정렬: "활성 여부" 머리칸만 {@code aria-sort} 가 없어 낭독기가 정렬 가능 여부 · 현재 상태를 못 들었다. 방향 글자(asc · desc)는
 * {@code aria-sort} 가 이미 전하는 상태라 접근성 이름에 섞이면 안 된다.
 */
class MonthlyReferenceNarrowAndSortTest {

  private static final String TEMPLATE_PATH =
      "src/main/jte/stock/fragments/monthlyDividendReference.jte";

  private static String template() throws IOException {
    return Files.readString(Path.of(TEMPLATE_PATH), StandardCharsets.UTF_8);
  }

  /** 주석을 걷어낸 본문(주석에 까닭을 적어 두면 글자 대조가 헛돈다). */
  private static List<String> markupLines() throws IOException {
    return template()
        .replaceAll("(?s)<%--.*?--%>", "")
        .lines()
        .map(String::trim)
        .filter(line -> !line.isEmpty())
        .toList();
  }

  @Test
  void 좁은_폭에서_접히지_않는_상자가_없다() throws IOException {
    List<String> lines = markupLines();

    List<String> buttons = lines.stream().filter(line -> line.contains("<button")).toList();
    assertThat(buttons).as("전제: 이 화면에 단추가 있다").hasSizeGreaterThanOrEqualTo(5);
    assertThat(buttons.stream().filter(line -> !line.contains("whitespace-normal")).toList())
        .as("단추는 한 줄 고정이라 긴 글자가 문서 폭을 늘린다 - 접히게 둘 것")
        .isEmpty();

    List<String> fields =
        lines.stream()
            .filter(
                line ->
                    line.contains("<input ")
                        || line.contains("<select ")
                        || line.contains("<textarea "))
            .filter(line -> !line.contains("type=\"hidden\""))
            .toList();
    assertThat(fields).as("전제: 이 화면에 입력칸이 있다").hasSizeGreaterThanOrEqualTo(8);
    assertThat(fields.stream().filter(line -> !line.contains("min-w-0")).toList())
        .as("입력칸은 size=20 만큼의 내재 폭이 있어 w-full 로는 안 줄어든다")
        .isEmpty();

    assertThat(template())
        .as("카드 제목 배지도 접혀야 한다(종목명이 길다)")
        .contains("badge badge-ghost badge-sm ml-2 align-middle h-auto whitespace-normal");
  }

  @Test
  void 정렬_머리칸은_모두_상태를_알린다() throws IOException {
    List<String> lines = markupLines();

    List<String> sortHeaders =
        lines.stream().filter(line -> line.contains("<th scope=\"col\"")).toList();
    // 행 링크도 정렬을 싣고 다니므로 머리칸 링크(기준 주소로 시작하는 것)만 센다.
    long sortable =
        lines.stream().filter(line -> line.contains("referenceListBaseUrl}&profileSort=")).count();
    assertThat(sortable).as("전제: 프로필 표에 정렬 링크가 여섯").isEqualTo(6);

    assertThat(sortHeaders.stream().filter(line -> line.contains("aria-sort")).count())
        .as("정렬 링크가 있는 머리칸은 모두 aria-sort 를 가져야 한다 - 하나만 빠져도 그 열만 조용하다")
        .isGreaterThanOrEqualTo(6);

    assertThat(template())
        .as("방향 글자(asc · desc)는 aria-sort 가 이미 전하는 상태다 - 접근성 이름에 섞이면 \"표시 순서 asc\" 로 읽힌다")
        .doesNotContain(
            "<span class=\"ml-1 text-xs uppercase text-base-content/60\">${profileDirectionValue}</span>")
        .contains(
            "<span class=\"ml-1 text-xs uppercase text-base-content/60\" aria-hidden=\"true\">${profileDirectionValue}</span>");
  }
}
