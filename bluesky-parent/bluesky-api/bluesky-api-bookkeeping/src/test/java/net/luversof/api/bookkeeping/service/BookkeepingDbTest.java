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
import net.luversof.api.bookkeeping.constant.RecurringCycle;
import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.domain.Book;
import net.luversof.api.bookkeeping.domain.BookInvite;
import net.luversof.api.bookkeeping.domain.Category;
import net.luversof.api.bookkeeping.repository.EntryQuery;
import net.luversof.api.bookkeeping.service.AssetService.AssetCommand;
import net.luversof.api.bookkeeping.service.BookService.BookCommand;
import net.luversof.api.bookkeeping.service.BudgetService.BudgetCommand;
import net.luversof.api.bookkeeping.service.CategoryService.CategoryCommand;
import net.luversof.api.bookkeeping.service.EntryService.EntrySearch;
import net.luversof.api.bookkeeping.service.FavoriteService.FavoriteCommand;
import net.luversof.api.bookkeeping.service.FxRateService.FxRateCommand;
import net.luversof.api.bookkeeping.service.RecurringService.RecurringCommand;

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
  @Autowired BudgetService budgetService;
  @Autowired FavoriteService favoriteService;
  @Autowired RecurringService recurringService;
  @Autowired FxRateService fxRateService;
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
  void transferOwnerSwapsRoles() {
    var owner = UUID.randomUUID();
    var book = newBook(owner, 1);
    var friend = join(book, owner);
    assertError(
        () -> memberService.transferOwner(book.id(), owner, owner),
        BookkeepingErrorCode.INVALID_REQUEST);
    assertError(
        () -> memberService.transferOwner(book.id(), owner, UUID.randomUUID()),
        BookkeepingErrorCode.NOT_EXIST_MEMBER);
    assertForbidden(
        () -> memberService.transferOwner(book.id(), friend, friend),
        BookkeepingErrorCode.NOT_BOOK_OWNER);

    assertThat(memberService.transferOwner(book.id(), owner, friend).role())
        .isEqualTo(BookRole.OWNER);
    var roles =
        memberService.members(book.id(), friend).stream()
            .collect(Collectors.toMap(m -> m.userId(), m -> m.role()));
    assertThat(roles).containsEntry(friend, BookRole.OWNER).containsEntry(owner, BookRole.MEMBER);
    // 예전 OWNER 는 이제 OWNER 일을 못 하고, 새 OWNER 는 나갈 수 없다
    assertForbidden(
        () -> bookService.delete(book.id(), owner), BookkeepingErrorCode.NOT_BOOK_OWNER);
    assertError(
        () -> memberService.remove(book.id(), friend, friend),
        BookkeepingErrorCode.OWNER_CANNOT_LEAVE);
    memberService.remove(book.id(), owner, owner); // 예전 OWNER 는 나갈 수 있다
    bookService.delete(book.id(), friend);
  }

  // ---- 0원

  @Test
  void zeroAmountExpenseRecurringAndFavoriteSaved() {
    var owner = UUID.randomUUID();
    var book = newBook(owner, 1);
    var cash = assetService.list(book.id(), owner).get(0).asset();
    var food =
        categoryService.list(book.id(), owner).stream()
            .filter(c -> c.kind() == CategoryKind.EXPENSE)
            .findFirst()
            .orElseThrow();
    var e =
        entryService.create(
            book.id(),
            owner,
            new EntryCommand(
                EntryType.EXPENSE,
                LocalDate.of(2026, 11, 4),
                null,
                BigDecimal.ZERO,
                null,
                cash.id(),
                null,
                food.id(),
                "복지카드 점심",
                "12,000원"));
    assertThat(e.amount()).isEqualByComparingTo("0");
    recurringService.create(
        book.id(),
        owner,
        new RecurringCommand(
            EntryType.EXPENSE,
            BigDecimal.ZERO,
            null,
            cash.id(),
            null,
            food.id(),
            "복지카드",
            null,
            RecurringCycle.MONTHLY,
            1,
            null,
            null,
            LocalDate.of(2026, 11, 1),
            null));
    favoriteService.create(
        book.id(),
        owner,
        new FavoriteCommand(
            "복지카드",
            EntryType.EXPENSE,
            BigDecimal.ZERO,
            null,
            cash.id(),
            null,
            food.id(),
            null,
            null));
    assertThat(assetService.list(book.id(), owner).get(0).balance())
        .isEqualByComparingTo("0"); // 잔액은 그대로
    bookService.delete(book.id(), owner);
  }

  // ---- 가져오기

  @Test
  void importAllOrNothingWithRowNumber() {
    var owner = UUID.randomUUID();
    var book = newBook(owner, 1);
    var cash = assetService.list(book.id(), owner).get(0).asset();
    var food =
        categoryService.list(book.id(), owner).stream()
            .filter(c -> c.kind() == CategoryKind.EXPENSE)
            .findFirst()
            .orElseThrow();
    var ok =
        new EntryCommand(
            EntryType.EXPENSE,
            LocalDate.of(2026, 11, 1),
            null,
            new BigDecimal("1000"),
            null,
            cash.id(),
            null,
            food.id(),
            "a",
            null);
    var bad =
        new EntryCommand(
            EntryType.EXPENSE,
            LocalDate.of(2026, 11, 2),
            null,
            new BigDecimal("0.5"),
            null,
            cash.id(),
            null,
            food.id(),
            "b",
            null);
    assertThatThrownBy(() -> entryService.importAll(book.id(), owner, List.of(ok, ok, bad)))
        .isInstanceOf(BlueskyException.class)
        .satisfies(
            e -> {
              var be = (BlueskyException) e;
              assertThat(be.getErrorCode()).isEqualTo("BookkeepingErrorCode.IMPORT_ROW_INVALID");
              assertThat(be.getErrorMessageArgs()[0]).isEqualTo(3);
              assertThat(be.getErrorMessageArgs()[1].toString()).contains("금액");
            });
    var nov =
        new EntrySearch(LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), null, null, null);
    assertThat(entryService.search(book.id(), owner, nov)).isEmpty(); // 하나도 안 들어갔다
    assertThat(entryService.importAll(book.id(), owner, List.of(ok, ok))).isEqualTo(2);
    assertThat(entryService.search(book.id(), owner, nov)).hasSize(2);
    assertError(
        () -> entryService.importAll(book.id(), owner, List.of()),
        BookkeepingErrorCode.INVALID_REQUEST);
    bookService.delete(book.id(), owner);
  }

  // ---- 환율

  @Test
  void fxRateUpsertDeleteAndRules() {
    var owner = UUID.randomUUID();
    var book = newBook(owner, 1);
    var nov = YearMonth.of(2026, 11);
    fxRateService.set(book.id(), owner, new FxRateCommand(nov, "USD", new BigDecimal("1380.5")));
    fxRateService.set(
        book.id(), owner, new FxRateCommand(nov, "JPY", new BigDecimal("9.12345678")));
    var list =
        fxRateService.set(book.id(), owner, new FxRateCommand(nov, "USD", new BigDecimal("1390")));
    assertThat(list)
        .extracting(r -> r.currency() + "=" + r.rate().stripTrailingZeros().toPlainString())
        .containsExactly("JPY=9.12345678", "USD=1390");
    assertThat(fxRateService.list(book.id(), owner, nov.plusMonths(1))).isEmpty();

    assertError(
        () -> fxRateService.set(book.id(), owner, new FxRateCommand(nov, "KRW", BigDecimal.ONE)),
        BookkeepingErrorCode.INVALID_CURRENCY); // 기준통화
    assertError(
        () ->
            fxRateService.set(
                book.id(), owner, new FxRateCommand(nov, "USD", new BigDecimal("0.123456789"))),
        BookkeepingErrorCode.INVALID_FX_RATE);
    assertError(
        () -> fxRateService.set(book.id(), owner, new FxRateCommand(nov, "XYZ", BigDecimal.ONE)),
        BookkeepingErrorCode.INVALID_CURRENCY);
    assertForbidden(
        () -> fxRateService.list(book.id(), UUID.randomUUID(), nov),
        BookkeepingErrorCode.NOT_BOOK_MEMBER);

    assertThat(fxRateService.set(book.id(), owner, new FxRateCommand(nov, "JPY", null)))
        .extracting(r -> r.currency())
        .containsExactly("USD");
    bookService.delete(book.id(), owner);
    assertThat(
            bookkeepingJdbcClient
                .sql("SELECT COUNT(*) FROM \"BookFxRate\" WHERE \"book_id\" = ?")
                .param(book.id())
                .query(Long.class)
                .single())
        .isZero();
  }

  // ---- 카드 할부

  @Test
  void cardInstallmentStoredAndBilled() {
    var owner = UUID.randomUUID();
    var book = newBook(owner, 1);
    var card =
        assetService.create(
            book.id(), owner, new AssetCommand(AssetKind.CREDIT_CARD, "카드", "KRW", null, 15, null));
    var food =
        categoryService.list(book.id(), owner).stream()
            .filter(c -> c.kind() == CategoryKind.EXPENSE)
            .findFirst()
            .orElseThrow();
    var tv =
        entryService.create(
            book.id(),
            owner,
            new EntryCommand(
                EntryType.EXPENSE,
                LocalDate.of(2026, 10, 10),
                null,
                new BigDecimal("100000"),
                null,
                card.id(),
                null,
                food.id(),
                "TV",
                null,
                3));
    assertThat(tv.installmentMonths()).isEqualTo(3);
    entryService.create(
        book.id(),
        owner,
        new EntryCommand(
            EntryType.EXPENSE,
            LocalDate.of(2026, 10, 20),
            null,
            new BigDecimal("5000"),
            null,
            card.id(),
            null,
            food.id(),
            null,
            null));
    var found =
        entryService.search(
            book.id(),
            owner,
            new EntrySearch(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31), null, null, null));
    assertThat(found).extracting(e -> e.installmentMonths()).containsExactlyInAnyOrder(3, null);
    assertThat(assetService.cardBills(book.id(), owner, YearMonth.of(2026, 11)))
        .singleElement()
        .satisfies(
            b -> {
              assertThat(b.assetId()).isEqualTo(card.id());
              assertThat(b.lumpSum()).isEqualByComparingTo("5000");
              assertThat(b.installments()).isEqualByComparingTo("33334");
              assertThat(b.total()).isEqualByComparingTo("38334");
            });
    // 잔액은 산 날 전액
    assertThat(
            assetService.list(book.id(), owner).stream()
                .filter(a -> a.asset().id().equals(card.id()))
                .findFirst()
                .orElseThrow()
                .balance())
        .isEqualByComparingTo("-105000");
    bookService.delete(book.id(), owner);
  }

  // ---- 반복

  @Test
  void recurringDueConfirmOnceAndReappearWhenEntryDeleted() {
    var owner = UUID.randomUUID();
    var book = newBook(owner, 1);
    var cash = assetService.list(book.id(), owner).get(0).asset();
    var rent =
        categoryService.list(book.id(), owner).stream()
            .filter(c -> c.kind() == CategoryKind.EXPENSE)
            .findFirst()
            .orElseThrow();
    var monthly =
        recurringService.create(
            book.id(),
            owner,
            new RecurringCommand(
                EntryType.EXPENSE,
                new BigDecimal("500000"),
                null,
                cash.id(),
                null,
                rent.id(),
                "월세",
                null,
                RecurringCycle.MONTHLY,
                31,
                3,
                7,
                LocalDate.of(2026, 1, 31),
                null));
    // 주기에 맞지 않는 칸은 버린다
    assertThat(monthly.dayOfWeek()).isNull();
    assertThat(monthly.monthOfYear()).isNull();

    var feb =
        recurringService.due(book.id(), owner, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31));
    assertThat(feb)
        .extracting(d -> d.date().toString())
        .containsExactly("2026-02-28", "2026-03-31");

    var entry = recurringService.confirm(book.id(), owner, monthly.id(), LocalDate.of(2026, 2, 28));
    assertThat(entry.entryDate()).isEqualTo(LocalDate.of(2026, 2, 28));
    assertThat(entry.content()).isEqualTo("월세");
    assertThat(
            recurringService.due(
                book.id(), owner, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 3, 31)))
        .extracting(d -> d.date().toString())
        .containsExactly("2026-03-31");
    assertError(
        () -> recurringService.confirm(book.id(), owner, monthly.id(), LocalDate.of(2026, 2, 28)),
        BookkeepingErrorCode.RECURRING_ALREADY_DONE);
    assertError(
        () -> recurringService.confirm(book.id(), owner, monthly.id(), LocalDate.of(2026, 2, 27)),
        BookkeepingErrorCode.NOT_RECURRING_OCCURRENCE);

    // 넣은 내역을 지우면 그 회차가 다시 보인다
    entryService.delete(book.id(), owner, entry.id());
    assertThat(
            recurringService.due(
                book.id(), owner, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)))
        .hasSize(1);

    assertError(
        () ->
            recurringService.create(
                book.id(),
                owner,
                new RecurringCommand(
                    EntryType.EXPENSE,
                    BigDecimal.TEN,
                    null,
                    cash.id(),
                    null,
                    rent.id(),
                    null,
                    null,
                    RecurringCycle.WEEKLY,
                    null,
                    8,
                    null,
                    LocalDate.of(2026, 1, 1),
                    null)),
        BookkeepingErrorCode.INVALID_RECURRING_CYCLE);
    assertError(
        () ->
            recurringService.create(
                book.id(),
                owner,
                new RecurringCommand(
                    EntryType.EXPENSE,
                    BigDecimal.TEN,
                    null,
                    cash.id(),
                    null,
                    null,
                    null,
                    null,
                    RecurringCycle.WEEKLY,
                    null,
                    1,
                    null,
                    LocalDate.of(2026, 1, 1),
                    null)),
        BookkeepingErrorCode.INVALID_ENTRY_COLUMNS);
    assertError(
        () ->
            recurringService.due(
                book.id(), owner, LocalDate.of(2026, 1, 1), LocalDate.of(2027, 6, 1)),
        BookkeepingErrorCode.INVALID_REQUEST);
    assertForbidden(
        () -> recurringService.list(book.id(), UUID.randomUUID()),
        BookkeepingErrorCode.NOT_BOOK_MEMBER);

    recurringService.confirm(book.id(), owner, monthly.id(), LocalDate.of(2026, 3, 31));
    bookService.delete(book.id(), owner);
    assertThat(
            bookkeepingJdbcClient
                .sql("SELECT COUNT(*) FROM \"RecurringDone\" WHERE \"recurring_id\" = ?")
                .param(monthly.id())
                .query(Long.class)
                .single())
        .isZero();
  }

  // ---- 즐겨찾기

  @Test
  void favoriteKeepsOnlyColumnsOfItsTypeAndChecksRules() {
    var owner = UUID.randomUUID();
    var book = newBook(owner, 1);
    var cash = assetService.list(book.id(), owner).get(0).asset();
    var categories = categoryService.list(book.id(), owner);
    var food =
        categories.stream().filter(c -> c.kind() == CategoryKind.EXPENSE).findFirst().orElseThrow();
    var salary =
        categories.stream().filter(c -> c.kind() == CategoryKind.INCOME).findFirst().orElseThrow();

    var lunch =
        favoriteService.create(
            book.id(),
            owner,
            new FavoriteCommand(
                " 점심 ",
                EntryType.EXPENSE,
                new BigDecimal("9000"),
                BigDecimal.ONE,
                cash.id(),
                cash.id(),
                food.id(),
                " ",
                "메모"));
    assertThat(lunch.name()).isEqualTo("점심");
    assertThat(lunch.toAmount()).isNull(); // 지출에 받는 칸은 버린다
    assertThat(lunch.toAssetId()).isNull();
    assertThat(lunch.content()).isNull();
    // 값 없이 이름 · 종류만
    var bare =
        favoriteService.create(
            book.id(),
            owner,
            new FavoriteCommand(
                "빈 틀", EntryType.TRANSFER, null, null, null, null, food.id(), null, null));
    assertThat(bare.categoryId()).isNull(); // 이체에 분류는 버린다
    assertThat(favoriteService.list(book.id(), owner))
        .extracting(f -> f.name() + ":" + f.displayOrder())
        .containsExactly("점심:0", "빈 틀:1");

    assertError(
        () ->
            favoriteService.create(
                book.id(),
                owner,
                new FavoriteCommand(
                    "x", EntryType.EXPENSE, null, null, null, null, salary.id(), null, null)),
        BookkeepingErrorCode.CATEGORY_KIND_MISMATCH);
    assertError(
        () ->
            favoriteService.create(
                book.id(),
                owner,
                new FavoriteCommand(
                    "x",
                    EntryType.EXPENSE,
                    new BigDecimal("0.5"),
                    null,
                    cash.id(),
                    null,
                    null,
                    null,
                    null)),
        BookkeepingErrorCode.INVALID_AMOUNT);
    assertError(
        () ->
            favoriteService.create(
                book.id(),
                owner,
                new FavoriteCommand(
                    " ", EntryType.EXPENSE, null, null, null, null, null, null, null)),
        BookkeepingErrorCode.INVALID_NAME);
    assertError(
        () ->
            favoriteService.create(
                book.id(),
                owner,
                new FavoriteCommand(
                    "x", EntryType.EXPENSE, null, null, UUID.randomUUID(), null, null, null, null)),
        BookkeepingErrorCode.NOT_EXIST_ASSET);
    assertForbidden(
        () -> favoriteService.list(book.id(), UUID.randomUUID()),
        BookkeepingErrorCode.NOT_BOOK_MEMBER);

    favoriteService.delete(book.id(), owner, bare.id());
    assertThat(favoriteService.list(book.id(), owner))
        .extracting(f -> f.id())
        .containsExactly(lunch.id());
    bookService.delete(book.id(), owner);
    assertThat(
            bookkeepingJdbcClient
                .sql("SELECT COUNT(*) FROM \"BookFavorite\" WHERE \"book_id\" = ?")
                .param(book.id())
                .query(Long.class)
                .single())
        .isZero();
  }

  // ---- 예산

  @Test
  void budgetUpsertDeleteAndCategoryRules() {
    var owner = UUID.randomUUID();
    var book = newBook(owner, 1);
    var categories = categoryService.list(book.id(), owner);
    var food =
        categories.stream()
            .filter(c -> c.kind() == CategoryKind.EXPENSE && c.parentId() == null)
            .findFirst()
            .orElseThrow();
    var salary =
        categories.stream().filter(c -> c.kind() == CategoryKind.INCOME).findFirst().orElseThrow();
    var snack =
        categoryService.create(book.id(), owner, new CategoryCommand(food.id(), null, "간식", null));

    budgetService.set(book.id(), owner, new BudgetCommand(null, new BigDecimal("500000")));
    budgetService.set(book.id(), owner, new BudgetCommand(food.id(), new BigDecimal("200000")));
    // 같은 자리에 다시 넣으면 바뀐다(행이 늘지 않는다)
    var list =
        budgetService.set(book.id(), owner, new BudgetCommand(null, new BigDecimal("600000")));
    assertThat(list)
        .extracting(b -> b.categoryId() + "=" + b.amount().stripTrailingZeros().toPlainString())
        .containsExactlyInAnyOrder("null=600000", food.id() + "=200000");

    assertError(
        () -> budgetService.set(book.id(), owner, new BudgetCommand(snack.id(), BigDecimal.TEN)),
        BookkeepingErrorCode.CATEGORY_DEPTH_EXCEEDED);
    assertError(
        () -> budgetService.set(book.id(), owner, new BudgetCommand(salary.id(), BigDecimal.TEN)),
        BookkeepingErrorCode.CATEGORY_KIND_MISMATCH);
    assertError(
        () -> budgetService.set(book.id(), owner, new BudgetCommand(null, new BigDecimal("0.5"))),
        BookkeepingErrorCode.INVALID_AMOUNT); // KRW 소수
    assertForbidden(
        () -> budgetService.list(book.id(), UUID.randomUUID()),
        BookkeepingErrorCode.NOT_BOOK_MEMBER);

    // 0 이면 지운다
    assertThat(budgetService.set(book.id(), owner, new BudgetCommand(null, BigDecimal.ZERO)))
        .extracting(b -> b.categoryId())
        .containsExactly(food.id());
    // 분류를 지우면 그 예산도
    var empty =
        categoryService.create(
            book.id(), owner, new CategoryCommand(null, CategoryKind.EXPENSE, "빈", null));
    budgetService.set(book.id(), owner, new BudgetCommand(empty.id(), BigDecimal.TEN));
    assertThat(categoryService.delete(book.id(), owner, empty.id())).isTrue();
    assertThat(budgetService.list(book.id(), owner))
        .extracting(b -> b.categoryId())
        .containsExactly(food.id());

    bookService.delete(book.id(), owner);
    assertThat(
            bookkeepingJdbcClient
                .sql("SELECT COUNT(*) FROM \"Budget\" WHERE \"book_id\" = ?")
                .param(book.id())
                .query(Long.class)
                .single())
        .isZero();
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
