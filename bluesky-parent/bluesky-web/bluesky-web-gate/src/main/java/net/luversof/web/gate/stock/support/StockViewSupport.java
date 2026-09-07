package net.luversof.web.gate.stock.support;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.util.StringUtils;

import io.github.luversof.boot.exception.BlueskyErrorMessage;
import io.github.luversof.boot.exception.BlueskyException;
import io.github.luversof.boot.exception.ErrorMessage;
import jakarta.servlet.http.HttpServletRequest;
import net.luversof.client.user.util.UserUtil;
import net.luversof.web.gate.stock.domain.StockItem;

/**
 * 주식 화면 컨트롤러들이 함께 쓰는 잔손질.
 *
 * <p>화면 컨트롤러를 쪼개면서 이 넷이 <b>양쪽에 다 필요</b>해졌다. 복제하면 로그인 판정이나 되돌아갈 주소 같은 것이 두 벌이 되어 한쪽만 고쳐지는 자리가 생긴다.
 */
public final class StockViewSupport {

  private StockViewSupport() {}

  /** 로그인하지 않았는지. */
  public static boolean isNotAuthenticated() {
    return UserUtil.getUserId() == null;
  }

  /**
   * 로그인 화면으로 보내는 <b>뷰 이름</b>. 돌아올 주소를 붙인다.
   *
   * <p>이름이 Url 이지만 돌려주는 것은 {@code redirect:} 뷰 이름이다 &mdash; 원본이 그랬고, 옮기면서 URL 만 돌려주게 바꿨더니 로그인 안 한
   * 요청이 뷰 이름 대신 주소 문자열을 반환하게 됐다. 그래서 이름을 View 로 바꿔 뜻을 맞춘다.
   */
  public static String loginRedirectView(HttpServletRequest request) {
    StringBuilder urlBuilder = new StringBuilder();
    urlBuilder.append(request.getScheme()).append("://").append(request.getServerName());
    int serverPort = request.getServerPort();
    if (serverPort != 80 && serverPort != 443) {
      urlBuilder.append(":").append(serverPort);
    }
    urlBuilder.append(request.getRequestURI());
    if (request.getQueryString() != null) {
      urlBuilder.append("?").append(request.getQueryString());
    }
    String encodedUrl =
        java.net.URLEncoder.encode(urlBuilder.toString(), java.nio.charset.StandardCharsets.UTF_8);
    return "redirect:/login?redirectUrl=" + encodedUrl;
  }

  public static String safeString(String value) {
    return value != null ? value : "";
  }

  /** 종목 목록을 종목코드 순으로. 대소문자를 가리지 않는다. */
  public static List<StockItem> sortedBySymbol(List<StockItem> stockItems) {
    if (stockItems == null || stockItems.isEmpty()) {
      return List.of();
    }
    return stockItems.stream()
        .filter(stockItem -> stockItem != null)
        .sorted(
            Comparator.comparing(
                stockItem -> safeString(stockItem.symbol()), String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  public static void appendQueryParam(StringBuilder redirectUrl, String key, Object value) {
    if (value == null) {
      return;
    }

    String text = String.valueOf(value).trim();
    if (!StringUtils.hasText(text)) {
      return;
    }

    redirectUrl.append('&').append(key).append('=');
    redirectUrl.append(URLEncoder.encode(text, StandardCharsets.UTF_8));
  }

  public static void requireNonNegative(BigDecimal value, String message) {
    if (value == null || value.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * 사용자에게 보여줄 실패 문구.
   *
   * <p>백엔드가 사유를 알려줬으면 그것을 그대로 쓰고, 아니면 결과만 말한다. 예전에는 원인을 모르는 경우에도 "입력값을 확인해 주세요"라고 적어 입력 탓으로 돌렸는데, 이
   * 경로는 지역 검증({@code IllegalArgumentException})이 이미 걸러낸 뒤라 정의상 입력 문제가 아닌 실패다 (연결 실패·서버 오류 등). 원인을
   * 모를 때 아는 척하지 않는다.
   */
  public static String failureMessage(Throwable throwable, String fallback) {
    String remote = remoteDisplayableMessage(throwable);
    return remote != null ? remote : fallback;
  }

  /**
   * 백엔드가 알려준 실패 사유를 꺼낸다. 없으면 {@code null}.
   *
   * <p>api-stock 호출이 실패하면 bluesky-boot 의 {@code BlueskyClientResponseErrorHandler} 가 응답 본문을 {@link
   * BlueskyException} 으로 바꿔 던진다. 그 안에는 백엔드가 "사용자에게 보여도 되는 문구"로 표시한 실제 사유가 들어 있는데, 화면은 그것을 버리고 "입력값을
   * 확인해 주세요" 같은 문구로 덮고 있었다. 원인을 아는데도 모른다고 말하는 셈이다.
   *
   * <p>{@code displayableMessage} 가 아닌 메시지는 내부용(예외 클래스명 등)이라 그대로 보여주지 않는다.
   */
  public static String remoteDisplayableMessage(Throwable throwable) {
    if (!(throwable instanceof BlueskyException blueskyException)) {
      return null;
    }
    List<ErrorMessage> candidates = new ArrayList<>();
    if (blueskyException.getErrorMessage() != null) {
      candidates.add(blueskyException.getErrorMessage());
    }
    if (blueskyException.getErrorMessageList() != null) {
      candidates.addAll(blueskyException.getErrorMessageList());
    }
    for (ErrorMessage candidate : candidates) {
      if (candidate instanceof BlueskyErrorMessage errorMessage
          && errorMessage.isDisplayableMessage()
          && errorMessage.getMessage() != null
          && !errorMessage.getMessage().isBlank()) {
        return errorMessage.getMessage();
      }
    }
    return null;
  }
}
