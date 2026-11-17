package net.luversof.web.dynamiccrud2.domain;

/** 입력 폼 형식. 필드 자동 등록 때 DB 타입으로 정하고 사용자가 바꿀 수 있다. */
public enum InputType {
  TEXT,
  TEXTAREA,
  NUMBER,
  DATE,
  DATETIME,
  BOOLEAN
}
