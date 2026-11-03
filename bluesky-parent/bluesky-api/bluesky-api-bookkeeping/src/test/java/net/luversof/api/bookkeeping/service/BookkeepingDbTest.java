package net.luversof.api.bookkeeping.service;

import static net.luversof.api.bookkeeping.service.RulesTest.assertError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.jdbc.core.JdbcAggregateOperations;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import io.github.luversof.boot.exception.BlueskyException;
import io.github.luversof.boot.uuid.UuidGenerator;
import net.luversof.api.bookkeeping.config.BookkeepingDataJdbcConfig;
import net.luversof.api.bookkeeping.constant.AssetKind;
import net.luversof.api.bookkeeping.constant.BookRole;
import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
import net.luversof.api.bookkeeping.constant.CategoryKind;
import net.luversof.api.bookkeeping.constant.EntryType;
import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.domain.Book;
import net.luversof.api.bookkeeping.domain.BookInvite;
import net.luversof.api.bookkeeping.domain.Category;
import net.luversof.api.bookkeeping.repository.EntryQuery;
import net.luversof.api.bookkeeping.service.AssetService.AssetCommand;
import net.luversof.api.bookkeeping.service.BookService.BookCommand;
import net.luversof.api.bookkeeping.service.CategoryService.CategoryCommand;
import net.luversof.api.bookkeeping.service.EntryService.EntrySearch;

/**
 * 실제 PostgreSQL 에 붙여 repository · 서비스를 끝까지 돌리는 시험. 평소 빌드에선 건너뛴다.
 *
 * <p>실행: {@code -Dbookkeeping.db.url=jdbc:postgresql://localhost:5432/postgres
 * -Dbookkeeping.db.user=... -Dbookkeeping.db.password=...}. 시작할 때 schema-create(IF NOT EXISTS)를
 * 실행한다. 버리는 DB 에서만 돌릴 것 — 시험마다 새 가계부를 만들고 끝에 지우지만 실패하면 행이 남는다.
 */
@EnabledIfSystemProperty(named = "bookkeeping.db.url", matches = ".+")
@SpringJUnitConfig(BookkeepingDbTest.Config.class)
class BookkeepingDbTest {

  @Configuration
  @EnableTransactionManagement
  @Import(BookkeepingDataJdbcConfig.class)
  @ComponentScan(basePackageClasses = {BookService.class, EntryQuery.class})
  static class Config extends AbstractJdbcConfiguration {

    @Bean
    DataSource routingDataSource() {
      return new DriverManagerDataSource(
          System.getProperty("bookkeeping.db.url"),
          System.getProperty("bookkeeping.db.user", "postgres"),
          System.getProperty("bookkeeping.db.password", ""));
    }

    @Bean
    NamedParameterJdbcOperations namedParameterJdbcOperations(DataSource routingDataSource) {
      return new NamedParameterJdbcTemplate(routingDataSource);
    }

    @Bean
    UuidGenerator uuidGenerator() {
      return UUID::randomUUID;
    }

    @Bean
    MessageSource messageSource() {
      var source = new ResourceBundleMessageSource();
      source.setBasename("bookkeepingMessage");
      return source;
    }
  }

  @Autowired BookService bookService;
  @Autowired AssetService assetService;
  @Autowired CategoryService categoryService;
  @Autowired EntryService entryService;
  @Autowired StatsService statsService;
  @Autowired MemberService memberService;
  @Autowired JdbcAggregateOperations jdbcAggregateOperations;
  @Autowired JdbcClient bookkeepingJdbcClient;

  @BeforeAll
  static void schema(@Autowired DataSource routingDataSource) throws Exception {
    try (var connection = routingDataSource.getConnection()) {
      ScriptUtils.executeSqlScript(
          connection,
          new FileSystemResource(
              Path.of(
                  "src/main/java/net/luversof/api/bookkeeping/schema-create-postgresql-bookkeeping.sql")));
    }
  }

  // ---- 가계부 · 권한

  @Test
  void createBookSeedsOwnerCategoriesAndCash() {
    var owner = UUID.randomUUID();
    var book = newBook(owner, 1);
    assertThat(bookService.list(owner))
        .singleElement()
        .satisfies(s -> assertThat(s.role()).isEqualTo(BookRole.OWNER));
    var categories = categoryService.list(book.id(), owner);
    assertThat(categories).hasSize(12);
    assertThat(categories.get(0).name()).isEqualTo("급여");
    assertThat(categories).filteredOn(c -> c.kind() == CategoryKind.EXPENSE).hasSize(8);
    assertThat(assetService.list(book.id(), owner))
        .singleElement()
        .satisfies(
            a -> {
              assertThat(a.asset().name()).isEqualTo("현금");
              assertThat(a.asset().currency()).isEqualTo("KRW");
              assertThat(a.balance()).isEqualByComparingTo("0");
            });
    bookService.delete(book.id(), owner);
  }

  @Test
  void nonMemberGets403AndMemberCannotManageBook() {
    var owner = UUID.randomUUID();
    var book = newBook(owner, 1);
    var stranger = UUID.randomUUID();
    assertForbidden(
        () -> assetService.list(book.id(), stranger), BookkeepingErrorCode.NOT_BOOK_MEMBER);
    assertForbidden(() -> bookService.get(book.id(), null), BookkeepingErrorCode.NOT_BOOK_MEMBER);

    var member = join(book, owner);
    assertThat(bookService.get(book.id(), member).name()).isEqualTo("test");
    assertForbidden(
        () -> bookService.update(book.id(), member, new BookCommand("x", "KRW", 1)),
        BookkeepingErrorCode.NOT_BOOK_OWNER);
    assertForbidden(
        () -> bookService.delete(book.id(), member), BookkeepingErrorCode.NOT_BOOK_OWNER);
    assertForbidden(
        () -> memberService.createInvite(book.id(), member), BookkeepingErrorCode.NOT_BOOK_OWNER);
    bookService.delete(book.id(), owner);
  }

  @Test
  void bookInputValidation() {
    var owner = UUID.randomUUID();
    assertError(
        () -> bookService.create(owner, new BookCommand(" ", "KRW", 1)),
        BookkeepingErrorCode.INVALID_NAME);
    assertError(
        () -> bookService.create(owner, new BookCommand("a", "won", 1)),
        BookkeepingErrorCode.INVALID_CURRENCY);
    assertError(
        () -> bookService.create(owner, new BookCommand("a", "KRW", 29)),
        BookkeepingErrorCode.INVALID_MONTH_START_DAY);
    assertThat(bookService.list(owner)).isEmpty();
  }

  // ---- 내역 · 잔액

  @Test
  void balancesFollowIncomeExpenseAndCrossCurrencyTransfer() {
    var owner = UUID.randomUUID();
    var book = newBook(owner, 1);
    var cash = cash(book, owner);
    var bank = asset(book, owner, AssetKind.BANK, "bank", "KRW", "100000");
    var usd = asset(book, owner, AssetKind.BANK, "usd", "USD", "0");
    var salary = category(book, owner, "급여");
    var food = category(book, owner, "식비");
    var day = LocalDate.of(2026, 11, 3);

    entryService.create(book.id(), owner, income(day, "3000000", bank.id(), salary.id()));
    entryService.create(book.id(), owner, expense(day, "12000", cash.id(), food.id(), "lunch"));
    entryService.create(book.id(), owner, transfer(day, "50000", null, bank.id(), cash.id()));
    var fx =
        entryService.create(
            book.id(), owner, transfer(day, "140000", "100.25", bank.id(), usd.id()));
    assertThat(fx.toAmount()).isEqualByComparingTo("100.25");

    var balances = balances(book, owner);
    assertThat(balances.get(bank.id())).isEqualByComparingTo("2910000");
    assertThat(balances.get(cash.id())).isEqualByComparingTo("38000");
    assertThat(balances.get(usd.id())).isEqualByComparingTo("100.25");

    // 다른 가계부의 자산 · 분류는 없는 것
    var other = newBook(owner, 1);
    assertError(
        () -> entryService.create(other.id(), owner, expense(day, "1", cash.id(), food.id(), null)),
        BookkeepingErrorCode.NOT_EXIST_ASSET);
    var otherCash = cash(other, owner);
    assertError(
        () ->
            entryService.create(
                other.id(), owner, expense(day, "1", otherCash.id(), food.id(), null)),
        BookkeepingErrorCode.NOT_EXIST_CATEGORY);

    bookService.delete(other.id(), owner);
    bookService.delete(book.id(), owner);
  }

  @Test
  void updateKeepsWriterAndDeleteRemoves() {
    var owner = UUID.randomUUID();
    var book = newBook(owner, 1);
    var member = join(book, owner);
    var cash = cash(book, owner);
    var food = category(book, owner, "식비");
    var day = LocalDate.of(2026, 11, 3);
    var e = entryService.create(book.id(), member, expense(day, "1000", cash.id(), food.id(), "a"));
    var updated =
        entryService.update(
            book.id(), owner, e.id(), expense(day, "2000", cash.id(), food.id(), "b"));
    assertThat(updated.createdBy()).isEqualTo(member);
    assertThat(updated.createdDate()).isEqualTo(e.createdDate());
    assertThat(balances(book, owner).get(cash.id())).isEqualByComparingTo("-2000");
    entryService.delete(book.id(), owner, e.id());
    assertThat(balances(book, owner).get(cash.id())).isEqualByComparingTo("0");
    assertError(
        () -> entryService.delete(book.id(), owner, e.id()), BookkeepingErrorCode.NOT_EXIST_ENTRY);
    bookService.delete(book.id(), owner);
  }

  @Test
  void searchByTextCategoryTreeAssetAndPeriod() {
    var owner = UUID.randomUUID();
    var book = newBook(owner, 1);
    var cash = cash(book, owner);
    var bank = asset(book, owner, AssetKind.BANK, "bank", "KRW", "0");
    var food = category(book, owner, "식비");
    var coffee =
        categoryService.create(book.id(), owner, new CategoryCommand(food.id(), null, "커피", null));
    var day = LocalDate.of(2026, 11, 3);
    entryService.create(book.id(), owner, expense(day, "9000", cash.id(), food.id(), "Lunch 100%"));
    entryService.create(
        book.id(), owner, expense(day.plusDays(1), "4500", cash.id(), coffee.id(), "latte"));
    entryService.create(book.id(), owner, transfer(day, "1000", null, bank.id(), cash.id()));
    entryService.create(
        book.id(), owner, expense(day.plusMonths(1), "1", bank.id(), food.id(), "later"));

    var month = new EntrySearch(day.withDayOfMonth(1), day.withDayOfMonth(30), null, null, null);
    assertThat(entryService.search(book.id(), owner, month)).hasSize(3);
    assertThat(entryService.search(book.id(), owner, month).get(0).content()).isEqualTo("latte");
    assertThat(search(book, owner, month, "lunch", null, null)).hasSize(1);
    assertThat(search(book, owner, month, "100%", null, null)).hasSize(1);
    assertThat(search(book, owner, month, "_", null, null)).isEmpty();
    assertThat(search(book, owner, month, null, food.id(), null)).hasSize(2);
    assertThat(search(book, owner, month, null, coffee.id(), null)).hasSize(1);
    assertThat(search(book, owner, month, null, null, bank.id())).hasSize(1);
    assertError(
        () ->
            entryService.search(
                book.id(), owner, new EntrySearch(day, day.minusDays(1), null, null, null)),
        BookkeepingErrorCode.INVALID_REQUEST);
    bookService.delete(book.id(), owner);
  }

  // ---- 통계

  @Test
  void monthStatsAndTrendUseMonthStartDay() {
    var owner = UUID.randomUUID();
    var book = newBook(owner, 25);
    var cash = cash(book, owner);
    var usd = asset(book, owner, AssetKind.CASH, "usd", "USD", "0");
    var salary = category(book, owner, "급여");
    var food = category(book, owner, "식비");
    // 2026-11 = 10-25 ~ 11-24
    entryService.create(
        book.id(), owner, income(LocalDate.of(2026, 10, 25), "3000000", cash.id(), salary.id()));
    entryService.create(
        book.id(), owner, expense(LocalDate.of(2026, 11, 24), "10000", cash.id(), food.id(), null));
    entryService.create(
        book.id(), owner, expense(LocalDate.of(2026, 11, 24), "5000", cash.id(), food.id(), null));
    entryService.create(
        book.id(), owner, expense(LocalDate.of(2026, 11, 24), "3.5", usd.id(), food.id(), null));
    // 2026-12 로 넘어감
    entryService.create(
        book.id(), owner, expense(LocalDate.of(2026, 11, 25), "7000", cash.id(), food.id(), null));

    var stats = statsService.month(book.id(), owner, YearMonth.of(2026, 11));
    assertThat(stats.range().from()).isEqualTo(LocalDate.of(2026, 10, 25));
    assertThat(stats.range().to()).isEqualTo(LocalDate.of(2026, 11, 24));
    assertThat(stats.daily())
        .extracting(
            d ->
                d.date()
                    + " "
                    + d.currency()
                    + " "
                    + d.income().stripTrailingZeros().toPlainString()
                    + " "
                    + d.expense().stripTrailingZeros().toPlainString())
        .containsExactly(
            "2026-10-25 KRW 3000000 0", "2026-11-24 KRW 0 15000", "2026-11-24 USD 0 3.5");
    assertThat(stats.categories())
        .extracting(
            c ->
                c.categoryId()
                    + " "
                    + c.currency()
                    + " "
                    + c.amount().stripTrailingZeros().toPlainString())
        .containsExactlyInAnyOrder(
            salary.id() + " KRW 3000000", food.id() + " KRW 15000", food.id() + " USD 3.5");

    var trend = statsService.trend(book.id(), owner, YearMonth.of(2026, 12), 3);
    assertThat(trend)
        .extracting(
            t ->
                t.month()
                    + " "
                    + t.currency()
                    + " "
                    + t.income().stripTrailingZeros().toPlainString()
                    + " "
                    + t.expense().stripTrailingZeros().toPlainString())
        .containsExactly(
            "2026-10 KRW 0 0", "2026-10 USD 0 0",
            "2026-11 KRW 3000000 15000", "2026-11 USD 0 3.5",
            "2026-12 KRW 0 7000", "2026-12 USD 0 0");
    assertError(
        () -> statsService.trend(book.id(), owner, YearMonth.of(2026, 12), 0),
        BookkeepingErrorCode.INVALID_REQUEST);
    bookService.delete(book.id(), owner);
  }

  // ---- 자산 · 분류 삭제 · 순서 · 통화 잠금

  @Test
  void assetDeleteHidesWhenUsedAndCurrencyLocks() {
    var owner = UUID.randomUUID();
    var book = newBook(owner, 1);
    var cash = cash(book, owner);
    var unused = asset(book, owner, AssetKind.BANK, "unused", "KRW", "0");
    var food = category(book, owner, "식비");
    entryService.create(
        book.id(), owner, expense(LocalDate.now(), "1", cash.id(), food.id(), null));

    assertError(
        () ->
            assetService.update(
                book.id(), owner, cash.id(), command(AssetKind.CASH, "현금", "USD", "0")),
        BookkeepingErrorCode.ASSET_CURRENCY_LOCKED);
    assertThat(
            assetService
                .update(
                    book.id(), owner, unused.id(), command(AssetKind.BANK, "unused", "USD", "0"))
                .currency())
        .isEqualTo("USD");

    assertThat(assetService.delete(book.id(), owner, cash.id())).isFalse();
    assertThat(assetService.delete(book.id(), owner, unused.id())).isTrue();
    assertThat(assetService.list(book.id(), owner))
        .singleElement()
        .satisfies(a -> assertThat(a.asset().hidden()).isTrue());
    bookService.delete(book.id(), owner);
  }

  @Test
  void assetMoveWithinKind() {
    var owner = UUID.randomUUID();
    var book = newBook(owner, 1);
    var a = asset(book, owner, AssetKind.BANK, "a", "KRW", "0");
    var b = asset(book, owner, AssetKind.BANK, "b", "KRW", "0");
    var c = asset(book, owner, AssetKind.BANK, "c", "KRW", "0");
    assetService.move(book.id(), owner, c.id(), true);
    assetService.move(book.id(), owner, a.id(), true); // 맨 위라 그대로
    assertThat(names(book, owner, AssetKind.BANK)).containsExactly("a", "c", "b");
    assetService.move(book.id(), owner, a.id(), false);
    assertThat(names(book, owner, AssetKind.BANK)).containsExactly("c", "a", "b");
    bookService.delete(book.id(), owner);
  }

  @Test
  void categoryTreeRulesAndDelete() {
    var owner = UUID.randomUUID();
    var book = newBook(owner, 1);
    var cash = cash(book, owner);
    var food = category(book, owner, "식비");
    var salary = category(book, owner, "급여");
    var coffee =
        categoryService.create(book.id(), owner, new CategoryCommand(food.id(), null, "커피", null));
    assertThat(coffee.kind()).isEqualTo(CategoryKind.EXPENSE);
    assertError(
        () ->
            categoryService.create(
                book.id(), owner, new CategoryCommand(coffee.id(), null, "x", null)),
        BookkeepingErrorCode.CATEGORY_DEPTH_EXCEEDED);
    assertError(
        () ->
            categoryService.create(
                book.id(), owner, new CategoryCommand(food.id(), CategoryKind.INCOME, "x", null)),
        BookkeepingErrorCode.CATEGORY_KIND_MISMATCH);

    // 소분류가 있으면 대분류와 함께 숨김
    assertThat(categoryService.delete(book.id(), owner, food.id())).isFalse();
    var byId = byId(categoryService.list(book.id(), owner));
    assertThat(byId.get(food.id()).hidden()).isTrue();
    assertThat(byId.get(coffee.id()).hidden()).isTrue();

    // 쓰인 분류는 숨김, 안 쓰인 소분류는 삭제
    entryService.create(book.id(), owner, income(LocalDate.now(), "1", cash.id(), salary.id()));
    assertThat(categoryService.delete(book.id(), owner, salary.id())).isFalse();
    assertThat(categoryService.delete(book.id(), owner, coffee.id())).isTrue();
    assertThat(byId(categoryService.list(book.id(), owner))).doesNotContainKey(coffee.id());
    bookService.delete(book.id(), owner);
  }

  // ---- 초대 · 멤버

  @Test
  void inviteAcceptOnceAndRejectsExpiredOrExistingMember() {
    var owner = UUID.randomUUID();
    var book = newBook(owner, 1);
    var invite = memberService.createInvite(book.id(), owner);
    assertThat(invite.token()).hasSize(43);
    assertThat(memberService.preview(invite.token()))
        .satisfies(
            p -> {
              assertThat(p.bookName()).isEqualTo("test");
              assertThat(p.expired()).isFalse();
              assertThat(p.used()).isFalse();
            });

    // 이미 멤버(OWNER)는 거절하고 링크를 쓰지 않는다
    assertError(
        () -> memberService.accept(invite.token(), owner),
        BookkeepingErrorCode.ALREADY_BOOK_MEMBER);
    var friend = UUID.randomUUID();
    assertThat(memberService.accept(invite.token(), friend).role()).isEqualTo(BookRole.MEMBER);
    assertThat(memberService.preview(invite.token()).used()).isTrue();
    assertError(
        () -> memberService.accept(invite.token(), UUID.randomUUID()),
        BookkeepingErrorCode.USED_INVITE);

    var expired =
        jdbcAggregateOperations.insert(
            new BookInvite(
                "expired-" + UUID.randomUUID(),
                book.id(),
                owner,
                Instant.now(),
                Instant.now().minusSeconds(1),
                null,
                null));
    assertThat(memberService.preview(expired.token()).expired()).isTrue();
    assertError(
        () -> memberService.accept(expired.token(), UUID.randomUUID()),
        BookkeepingErrorCode.EXPIRED_INVITE);
    assertError(() -> memberService.accept("nope", friend), BookkeepingErrorCode.NOT_EXIST_INVITE);

    assertThat(memberService.members(book.id(), friend))
        .extracting(m -> m.role())
        .containsExactly(BookRole.OWNER, BookRole.MEMBER);
    bookService.delete(book.id(), owner);
  }

  @Test
  void leaveKickAndOwnerCannotLeave() {
    var owner = UUID.randomUUID();
    var book = newBook(owner, 1);
    var a = join(book, owner);
    var b = join(book, owner);
    assertError(
        () -> memberService.remove(book.id(), owner, owner),
        BookkeepingErrorCode.OWNER_CANNOT_LEAVE);
    assertForbidden(
        () -> memberService.remove(book.id(), a, b), BookkeepingErrorCode.NOT_BOOK_OWNER);
    memberService.remove(book.id(), a, a);
    memberService.remove(book.id(), owner, b);
    assertError(
        () -> memberService.remove(book.id(), owner, b), BookkeepingErrorCode.NOT_EXIST_MEMBER);
    assertThat(memberService.members(book.id(), owner)).hasSize(1);
    assertThat(bookService.list(a)).isEmpty();
    bookService.delete(book.id(), owner);
  }

  @Test
  void deleteBookRemovesEveryRow() {
    var owner = UUID.randomUUID();
    var book = newBook(owner, 1);
    var cash = cash(book, owner);
    var food = category(book, owner, "식비");
    entryService.create(
        book.id(), owner, expense(LocalDate.now(), "1", cash.id(), food.id(), null));
    memberService.createInvite(book.id(), owner);
    join(book, owner);
    bookService.delete(book.id(), owner);
    for (var table : List.of("Book", "BookMember", "BookInvite", "Asset", "Category", "Entry")) {
      var column = table.equals("Book") ? "id" : "book_id";
      assertThat(
              bookkeepingJdbcClient
                  .sql("SELECT count(*) FROM \"" + table + "\" WHERE \"" + column + "\" = ?")
                  .param(book.id())
                  .query(Long.class)
                  .single())
          .as(table)
          .isZero();
    }
  }

  // ---- 도우미

  Book newBook(UUID owner, int startDay) {
    return bookService.create(owner, new BookCommand("test", "KRW", startDay));
  }

  UUID join(Book book, UUID owner) {
    var user = UUID.randomUUID();
    memberService.accept(memberService.createInvite(book.id(), owner).token(), user);
    return user;
  }

  Asset cash(Book book, UUID userId) {
    return assetService.list(book.id(), userId).stream()
        .map(AssetService.AssetBalance::asset)
        .filter(a -> a.kind() == AssetKind.CASH)
        .findFirst()
        .orElseThrow();
  }

  Asset asset(
      Book book, UUID userId, AssetKind kind, String name, String currency, String opening) {
    return assetService.create(book.id(), userId, command(kind, name, currency, opening));
  }

  static AssetCommand command(AssetKind kind, String name, String currency, String opening) {
    return new AssetCommand(kind, name, currency, new BigDecimal(opening), null, null);
  }

  Category category(Book book, UUID userId, String name) {
    return categoryService.list(book.id(), userId).stream()
        .filter(c -> c.name().equals(name))
        .findFirst()
        .orElseThrow();
  }

  Map<UUID, BigDecimal> balances(Book book, UUID userId) {
    return assetService.list(book.id(), userId).stream()
        .collect(Collectors.toMap(a -> a.asset().id(), AssetService.AssetBalance::balance));
  }

  List<String> names(Book book, UUID userId, AssetKind kind) {
    return assetService.list(book.id(), userId).stream()
        .map(AssetService.AssetBalance::asset)
        .filter(a -> a.kind() == kind)
        .map(Asset::name)
        .toList();
  }

  List<?> search(
      Book book, UUID userId, EntrySearch base, String q, UUID categoryId, UUID assetId) {
    return entryService.search(
        book.id(), userId, new EntrySearch(base.from(), base.to(), q, categoryId, assetId));
  }

  static Map<UUID, Category> byId(List<Category> list) {
    return list.stream().collect(Collectors.toMap(Category::id, Function.identity()));
  }

  static EntryCommand income(LocalDate day, String amount, UUID assetId, UUID categoryId) {
    return new EntryCommand(
        EntryType.INCOME,
        day,
        null,
        new BigDecimal(amount),
        null,
        assetId,
        null,
        categoryId,
        null,
        null);
  }

  static EntryCommand expense(
      LocalDate day, String amount, UUID assetId, UUID categoryId, String content) {
    return new EntryCommand(
        EntryType.EXPENSE,
        day,
        null,
        new BigDecimal(amount),
        null,
        assetId,
        null,
        categoryId,
        content,
        null);
  }

  static EntryCommand transfer(LocalDate day, String amount, String toAmount, UUID from, UUID to) {
    return new EntryCommand(
        EntryType.TRANSFER,
        day,
        null,
        new BigDecimal(amount),
        toAmount == null ? null : new BigDecimal(toAmount),
        from,
        to,
        null,
        null,
        null);
  }

  static void assertForbidden(
      org.assertj.core.api.ThrowableAssert.ThrowingCallable call, BookkeepingErrorCode code) {
    assertError(call, code);
    assertThatThrownBy(call).extracting(e -> ((BlueskyException) e).getStatus()).isEqualTo(403);
  }
}
