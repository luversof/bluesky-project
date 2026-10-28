package net.luversof.web.gate.stock.support;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * 월배당 기준 데이터(프로필 · 지급 이력)를 고칠 수 있는 사용자.
 *
 * <p>기준 데이터는 사용자 구분 없는 공용 값이라(symbol 만으로 저장 · 삭제) 로그인한 누구나 남의 화면 기준값을 지울 수 있었고, 출처 주소도 심을 수
 * 있었다(2026-10-27 감사). {@code stock.reference-editor-user-ids} 에 쉼표로 이은 userId 를 두면 그 사용자만 고친다. 비워
 * 두면 예전처럼 모두 허용한다 - 운영 사용자 id 를 모른 채 막으면 주인까지 막히므로, 기동 때 경고만 남긴다.
 */
@Component
public class MonthlyReferenceEditors {

  private static final Logger log = LoggerFactory.getLogger(MonthlyReferenceEditors.class);

  private final Set<UUID> editorUserIds;

  public MonthlyReferenceEditors(@Value("${stock.reference-editor-user-ids:}") String userIds) {
    this.editorUserIds = parse(userIds);
    if (editorUserIds.isEmpty()) {
      log.warn(
          "stock.reference-editor-user-ids is empty - every signed-in user can edit"
              + " monthly dividend reference data");
    }
  }

  static Set<UUID> parse(String userIds) {
    if (userIds == null || userIds.isBlank()) {
      return Set.of();
    }
    return Stream.of(userIds.split(","))
        .map(String::trim)
        .filter(id -> !id.isEmpty())
        .map(UUID::fromString)
        .collect(Collectors.toUnmodifiableSet());
  }

  /** 목록이 있으면 그 안의 사용자만. 비었으면 누구나. */
  public boolean canEdit(UUID userId) {
    return editorUserIds.isEmpty() || (userId != null && editorUserIds.contains(userId));
  }

  /** 고칠 수 없는 사용자면 403. */
  public void requireEditor(UUID userId) {
    if (!canEdit(userId)) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "monthly reference data is read-only");
    }
  }
}
