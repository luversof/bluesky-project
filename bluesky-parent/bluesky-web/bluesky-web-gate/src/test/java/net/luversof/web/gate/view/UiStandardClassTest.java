package net.luversof.web.gate.view;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * 버튼 · 배지 · 알림 · 카드 제목 · 링크 · 입력 · 탭의 기준 모양(사용자 선택 2026-11-10, /dev/ui-catalog: BT2 · BG1 · AL1 ·
 * CT1 · LK1 · IN2 · TB1).
 *
 * <p>이 요소들은 hx-* · data-* 속성이 많아 jte 조각으로 감싸지 않고, daisyUI 클래스 조합을 기준 한 벌로 묶는다(표의 .data-table 과 같은
 * 방식). 전수 검토 때 버튼 41 · 배지 45 · 링크 20 · 입력 17 · 알림 11 · 탭 8 · 카드 제목 7 가지로 갈라져 있었다. 화면이 기준 밖의 모양 클래스를
 * 다시 고르면 여기서 막는다. 정적 클래스만 본다 - ${...} 식 안의 클래스(선택된 기간 단추 등)는 그 자리의 동작이다.
 */
class UiStandardClassTest {

  private static final List<Path> ROOTS =
      List.of(Path.of("src/main/jte/stock"), Path.of("src/main/jte/_components/ui"));

  private static final Pattern START_TAG = Pattern.compile("<\\w+\\b");
  private static final Pattern EXPR = Pattern.compile("\\$\\{(?:[^{}]|\\{[^{}]*\\})*\\}");

  private record Tag(String where, Set<String> tokens, String raw, String tag) {}

  private static List<Tag> tags() throws IOException {
    List<Tag> out = new ArrayList<>();
    for (Path root : ROOTS) {
      try (Stream<Path> walk = Files.walk(root)) {
        for (Path p : walk.filter(x -> x.toString().endsWith(".jte")).toList()) {
          String src =
              Files.readString(p, StandardCharsets.UTF_8).replaceAll("(?s)<%--.*?--%>", "");
          Matcher m = START_TAG.matcher(src);
          while (m.find()) {
            int j = m.end();
            int depth = 0;
            while (j < src.length()) {
              char c = src.charAt(j);
              if (c == '{' && (src.charAt(j - 1) == '$' || depth > 0)) {
                depth++;
              } else if (c == '}' && depth > 0) {
                depth--;
              } else if (c == '>' && depth == 0) {
                break;
              }
              j++;
            }
            String tag = src.substring(m.start(), Math.min(j + 1, src.length()));
            Matcher cm =
                Pattern.compile(
                        "\\sclass=\"((?:[^\"$]|\\$\\{(?:[^{}]|\\{[^{}]*\\})*\\}|\\$(?!\\{))*)\"")
                    .matcher(tag);
            if (!cm.find()) {
              continue;
            }
            String value = cm.group(1);
            Set<String> tokens = Set.of(EXPR.matcher(value).replaceAll(" ").trim().split("\\s+"));
            String where =
                root.relativize(p).toString().replace('\\', '/')
                    + ":"
                    + src.substring(0, m.start()).split("\n", -1).length;
            out.add(new Tag(where, tokens, value, tag));
          }
        }
      }
    }
    return out;
  }

  private static List<String> offenders(Function<Tag, Boolean> bad) throws IOException {
    List<String> out = new ArrayList<>();
    for (Tag t : tags()) {
      if (bad.apply(t)) {
        out.add(t.where() + " " + t.tokens());
      }
    }
    return out;
  }

  @Test
  void 버튼은_주_동작_primary_보조_동작_outline_이다() throws IOException {
    Set<String> looks =
        Set.of("btn-primary", "btn-outline", "btn-error", "btn-warning", "btn-success", "btn-info");
    // 바탕 없는 단추(ghost)는 아이콘 단추 · 기간 이동 화살표만. 정렬 머리글은 sortButtonHeader 가 buttonClass 식으로 넘긴다.
    Set<String> iconOnly = Set.of("btn-circle", "btn-square", "date-range-prev", "date-range-next");
    assertThat(
            offenders(t -> {
              if (!t.tokens().contains("btn")) {
                return false;
              }
              boolean dynLook = t.raw().contains("\"btn-");
              boolean look =
                  dynLook
                      || t.tokens().stream().anyMatch(looks::contains)
                      || (t.tokens().contains("btn-ghost")
                          && (t.tokens().stream().anyMatch(iconOnly::contains)
                              || t.tag().contains("data-sort-key")));
              boolean size =
                  t.tokens().contains("btn-sm")
                      || t.tokens().contains("btn-xs")
                      || t.raw().contains("btn-sm")
                      || t.raw().contains("btn-xs");
              boolean banned =
                  t.tokens().contains("btn-secondary")
                      || t.tokens().contains("btn-md")
                      || t.tokens().contains("btn-lg");
              return !look || !size || banned;
            }))
        .as("BT2: 주 동작 btn-primary · 보조 btn-outline · 위험 btn-error, 크기 btn-sm(표 · 도구 막대 btn-xs)")
        .isEmpty();
  }

  @Test
  void 배지는_테두리_sm_이다() throws IOException {
    assertThat(
            offenders(t ->
                t.tokens().contains("badge")
                    && (!t.tokens().contains("badge-outline")
                        || !t.tokens().contains("badge-sm")
                        || t.tokens().contains("badge-xs")
                        || t.tokens().contains("badge-lg")
                        || t.tokens().contains("badge-ghost"))))
        .as("BG1: badge badge-sm badge-outline (+ 색)")
        .isEmpty();
  }

  @Test
  void 알림은_daisyUI_기본_한_벌이다() throws IOException {
    Pattern override =
        Pattern.compile(
            "bg-\\S+|border(-\\S+)?|text-(xs|sm|\\S+-content|base-content)|p[xy]?-\\S+");
    assertThat(
            offenders(t ->
                t.tokens().contains("alert")
                    && t.tokens().stream().anyMatch(x -> override.matcher(x).matches())))
        .as("AL1: alert alert-{error|warning|success|info} - 색 · 테두리 · 여백을 덧칠하지 않는다")
        .isEmpty();
  }

  @Test
  void 카드_제목은_크기를_따로_고르지_않는다() throws IOException {
    Set<String> sizes = Set.of("text-sm", "text-base", "text-lg", "text-xl", "text-2xl");
    assertThat(
            offenders(t ->
                t.tokens().contains("card-title") && t.tokens().stream().anyMatch(sizes::contains)))
        .as("CT1: .card-title 의 16px(main.css) 그대로")
        .isEmpty();
  }

  @Test
  void 링크는_마우스를_올리면_밑줄이다() throws IOException {
    assertThat(
            offenders(t ->
                t.tokens().contains("link")
                    && (!t.tokens().contains("link-hover")
                        || t.tokens().contains("no-underline")
                        || t.tokens().contains("link-primary"))))
        .as("LK1: link link-hover")
        .isEmpty();
  }

  @Test
  void 입력과_선택은_작은_크기다() throws IOException {
    assertThat(
            offenders(t ->
                (t.tokens().contains("input") && !t.tokens().contains("input-sm"))
                    || (t.tokens().contains("select") && !t.tokens().contains("select-sm"))))
        .as("IN2: input-sm · select-sm")
        .isEmpty();
  }

  @Test
  void 탭은_상자형_칸_나눔이다() throws IOException {
    Set<String> required = Set.of("tabs-box", "w-full", "bg-base-200/70", "p-1");
    assertThat(
            offenders(t ->
                t.tokens().contains("tabs")
                    && (!t.tokens().containsAll(required)
                        || t.tokens().contains("tabs-xs")
                        || t.tokens().contains("tabs-sm")
                        || t.tokens().contains("w-fit"))))
        .as("TB1: tabs tabs-box w-full bg-base-200/70 p-1")
        .isEmpty();
  }
}
