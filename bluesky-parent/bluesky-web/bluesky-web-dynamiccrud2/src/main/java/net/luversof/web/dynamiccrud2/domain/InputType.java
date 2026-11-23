package net.luversof.web.dynamiccrud2.domain;

/** 입력 폼 형식. 필드 자동 등록 때 DB 타입으로 정하고 사용자가 바꿀 수 있다. */
public enum InputType {
  TEXT,
  TEXTAREA,
  NUMBER,
  DATE,
  DATETIME,
  BOOLEAN;

  /**
   * 목록 칸 · 열 제목의 가로 정렬(숫자 · 참거짓 = 오른쪽). 칸(_components/cellValue)과 제목이 같이 쓴다. 클래스 이름이 여기 있어 Tailwind
   * 가 이 파일도 읽는다(frontend/main.css @source)
   */
  public String align() {
    return switch (this) {
      case NUMBER, BOOLEAN -> "text-right";
      default -> "";
    };
  }
}
