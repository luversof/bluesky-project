package net.luversof.web.gate.poe;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/** 삿된 분류 영어 이름(10-02 C15) — 파서가 만들 수 있는 한글 분류가 전부 표에 있는가. */
class PoeFoulbornCategoriesTest {

  private static final Path PARSER =
      Path.of("../../bluesky-api/bluesky-api-poe/tools/poe-extract/parse-foulborn.mjs");

  /** 파서의 두 표(CATEGORY_KO · UNIQUE_CATEGORY_KO) 안 한글 값. */
  private static Set<String> parserCategories() throws IOException {
    String source = Files.readString(PARSER, StandardCharsets.UTF_8);
    Set<String> out = new LinkedHashSet<>();
    Matcher block =
        Pattern.compile("const (?:UNIQUE_)?CATEGORY_KO = \\{(.*?)\\};", Pattern.DOTALL)
            .matcher(source);
    while (block.find()) {
      Matcher value = Pattern.compile("\"([\\uAC00-\\uD7A3]+)\"").matcher(block.group(1));
      while (value.find()) {
        out.add(value.group(1));
      }
    }
    return out;
  }

  @Test
  void 파서의_한글_분류는_모두_영어_이름이_있다() throws IOException {
    assumeTrue(Files.exists(PARSER), "API 도구 폴더가 없는 체크아웃 — 건너뜀");
    Set<String> categories = parserCategories();
    assertThat(categories)
        .as("파서 표를 못 읽었다 - 정규식이 무력하다")
        .hasSizeGreaterThan(20)
        .contains("갑옷", "낚싯대");
    assertThat(categories)
        .as("새 분류가 생겼다 - PoeFoulbornCategories 표에 영어 이름을 넣을 것")
        .allSatisfy(ko -> assertThat(PoeFoulbornCategories.table()).containsKey(ko));
  }

  @Test
  void 표에_없으면_한글_그대로_null_은_null() {
    assertThat(PoeFoulbornCategories.en("갑옷")).isEqualTo("Body Armours");
    assertThat(PoeFoulbornCategories.en("낚싯대")).isEqualTo("Fishing Rods");
    assertThat(PoeFoulbornCategories.en("새분류")).isEqualTo("새분류");
    assertThat(PoeFoulbornCategories.en(null)).isNull();
  }

  @Test
  void 영어_이름에_한글이_없다() {
    assertThat(PoeFoulbornCategories.table().values())
        .allSatisfy(en -> assertThat(en).doesNotContainPattern("[\\uAC00-\\uD7A3]"));
  }
}
