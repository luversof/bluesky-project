package net.luversof.api.bookkeeping.constant;

/** 가계부를 만들 때 넣는 기본 분류(대분류만). 이름은 메시지 파일에서 꺼내 문자열로 저장한다. */
public enum DefaultCategory {
  INCOME_SALARY(CategoryKind.INCOME),
  INCOME_BONUS(CategoryKind.INCOME),
  INCOME_INTEREST(CategoryKind.INCOME),
  INCOME_ETC(CategoryKind.INCOME),
  EXPENSE_FOOD(CategoryKind.EXPENSE),
  EXPENSE_TRANSPORTATION(CategoryKind.EXPENSE),
  EXPENSE_LIVING(CategoryKind.EXPENSE),
  EXPENSE_EDUCATION(CategoryKind.EXPENSE),
  EXPENSE_CULTURE(CategoryKind.EXPENSE),
  EXPENSE_SOCIAL(CategoryKind.EXPENSE),
  EXPENSE_MANAGEMENT_FEE(CategoryKind.EXPENSE),
  EXPENSE_ETC(CategoryKind.EXPENSE);

  private final CategoryKind kind;

  DefaultCategory(CategoryKind kind) {
    this.kind = kind;
  }

  public CategoryKind getKind() {
    return kind;
  }

  /** 메시지 키. */
  public String messageCode() {
    return "bookkeeping.default.category." + name();
  }
}
