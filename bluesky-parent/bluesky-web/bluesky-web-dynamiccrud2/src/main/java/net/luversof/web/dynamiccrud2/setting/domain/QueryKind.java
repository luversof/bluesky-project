package net.luversof.web.dynamiccrud2.setting.domain;

/** 메뉴 작업 종류. READ 는 메뉴당 1개, 나머지는 N 개. */
public enum QueryKind {
  READ,
  INSERT,
  UPDATE,
  DELETE
}
