package net.luversof.api.bookkeeping.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import io.github.luversof.boot.context.ApplicationContextUtil;
import io.github.luversof.boot.core.CoreBaseProperties;
import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.api.bookkeeping.constant.BookRole;
import net.luversof.api.bookkeeping.constant.EntryType;
import net.luversof.api.bookkeeping.domain.Book;
import net.luversof.api.bookkeeping.domain.Entry;
import net.luversof.api.bookkeeping.service.AssetService;
import net.luversof.api.bookkeeping.service.BookService;
import net.luversof.api.bookkeeping.service.BookService.BookSummary;
import net.luversof.api.bookkeeping.service.CategoryService;
import net.luversof.api.bookkeeping.service.EntryCommand;
import net.luversof.api.bookkeeping.service.EntryService;
import net.luversof.api.bookkeeping.service.EntryService.EntrySearch;
import net.luversof.api.bookkeeping.service.MemberService;
import net.luversof.api.bookkeeping.service.MonthRange;
import net.luversof.api.bookkeeping.service.StatsService;
import net.luversof.api.bookkeeping.service.StatsService.MonthStats;

/** 경로 · 파라미터 바인딩 · JSON 모양 · 잘못된 요청 400. 서비스는 가짜. */
class ControllerBindingTest {

  static final UUID USER = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
  static final UUID BOOK = UUID.fromString("00000000-0000-0000-0000-000000000001");

  BookService bookService = mock(BookService.class);
  AssetService assetService = mock(AssetService.class);
  CategoryService categoryService = mock(CategoryService.class);
  EntryService entryService = mock(EntryService.class);
  StatsService statsService = mock(StatsService.class);
  MemberService memberService = mock(MemberService.class);
  MockMvc mvc;

  @BeforeEach
  void setUp() {
    // ProblemDetailUtil 이 정적 ApplicationContextUtil 에서 CoreBaseProperties · MessageSourceAccessor 를
    // 꺼낸다(앱에선
    // bluesky-boot 가 넣음).
    var properties = new CoreBaseProperties();
    properties.setLogExceptExceptionList(List.of());
    var context = new StaticApplicationContext();
    context.getBeanFactory().registerSingleton("coreBaseProperties", properties);
    context.refresh();
    context
        .getBeanFactory()
        .registerSingleton("messageSourceAccessor", new MessageSourceAccessor(context));
    ApplicationContextUtil.setApplicationContext(context);
    mvc =
        MockMvcBuilders.standaloneSetup(
                new BookController(bookService),
                new AssetController(assetService),
                new CategoryController(categoryService),
                new EntryController(entryService),
                new StatsController(statsService),
                new MemberController(memberService))
            .setControllerAdvice(new BookkeepingRequestErrorHandler())
            .build();
  }

  @AfterEach
  void tearDown() {
    ApplicationContextUtil.setApplicationContext(null);
  }

  @Test
  void listBooksReturnsBookAndRole() throws Exception {
    when(bookService.list(USER))
        .thenReturn(
            List.of(
                new BookSummary(
                    new Book(BOOK, "우리집", "KRW", 25, Instant.parse("2026-11-03T00:00:00Z")),
                    BookRole.OWNER)));
    mvc.perform(get("/api/books").param("userId", USER.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].book.id").value(BOOK.toString()))
        .andExpect(jsonPath("$[0].book.name").value("우리집"))
        .andExpect(jsonPath("$[0].book.monthStartDay").value(25))
        .andExpect(jsonPath("$[0].book.createdDate").value("2026-11-03T00:00:00Z"))
        .andExpect(jsonPath("$[0].role").value("OWNER"));
  }

  @Test
  void createEntryBindsJsonBody() throws Exception {
    var asset = UUID.randomUUID();
    var category = UUID.randomUUID();
    when(entryService.create(eq(BOOK), eq(USER), any())).thenReturn(entry(asset, category));
    mvc.perform(
            post("/api/books/{bookId}/entries", BOOK)
                .param("userId", USER.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"type":"EXPENSE","entryDate":"2026-11-03","entryTime":"12:30",
                     "amount":12000,"assetId":"%s","categoryId":"%s","content":"점심"}
                    """
                        .formatted(asset, category)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.entryDate").value("2026-11-03"))
        .andExpect(jsonPath("$.type").value("EXPENSE"));
    var captor = ArgumentCaptor.forClass(EntryCommand.class);
    verify(entryService).create(eq(BOOK), eq(USER), captor.capture());
    var c = captor.getValue();
    assertThat(c.type()).isEqualTo(EntryType.EXPENSE);
    assertThat(c.entryDate()).isEqualTo(LocalDate.of(2026, 11, 3));
    assertThat(c.entryTime()).isEqualTo(LocalTime.of(12, 30));
    assertThat(c.amount()).isEqualByComparingTo("12000");
    assertThat(c.assetId()).isEqualTo(asset);
    assertThat(c.toAssetId()).isNull();
    assertThat(c.content()).isEqualTo("점심");
  }

  @Test
  void searchBindsDatesAndOptionalFilters() throws Exception {
    when(entryService.search(eq(BOOK), eq(USER), any())).thenReturn(List.of());
    var category = UUID.randomUUID();
    mvc.perform(
            get("/api/books/{bookId}/entries", BOOK)
                .param("userId", USER.toString())
                .param("from", "2026-10-25")
                .param("to", "2026-11-24")
                .param("q", "점심")
                .param("categoryId", category.toString()))
        .andExpect(status().isOk());
    verify(entryService)
        .search(
            BOOK,
            USER,
            new EntrySearch(
                LocalDate.of(2026, 10, 25), LocalDate.of(2026, 11, 24), "점심", category, null));
  }

  @Test
  void monthStatsBindsYearMonthAndShowsRange() throws Exception {
    when(statsService.month(BOOK, USER, YearMonth.of(2026, 11)))
        .thenReturn(
            new MonthStats(MonthRange.of(YearMonth.of(2026, 11), 25), List.of(), List.of()));
    mvc.perform(
            get("/api/books/{bookId}/stats/month", BOOK)
                .param("userId", USER.toString())
                .param("month", "2026-11"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.range.month").value("2026-11"))
        .andExpect(jsonPath("$.range.from").value("2026-10-25"))
        .andExpect(jsonPath("$.range.to").value("2026-11-24"));
  }

  @Test
  void trendDefaultsTo12Months() throws Exception {
    when(statsService.trend(BOOK, USER, null, 12)).thenReturn(List.of());
    mvc.perform(get("/api/books/{bookId}/stats/trend", BOOK).param("userId", USER.toString()))
        .andExpect(status().isOk());
    verify(statsService).trend(BOOK, USER, null, 12);
  }

  @Test
  void deleteAssetReportsHiddenOrDeleted() throws Exception {
    var asset = UUID.randomUUID();
    when(assetService.delete(BOOK, USER, asset)).thenReturn(false);
    mvc.perform(
            delete("/api/books/{bookId}/assets/{assetId}", BOOK, asset)
                .param("userId", USER.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.deleted").value(false));
  }

  @Test
  void moveAndMemberPathsReachService() throws Exception {
    var category = UUID.randomUUID();
    mvc.perform(
            post("/api/books/{bookId}/categories/{id}/move", BOOK, category)
                .param("userId", USER.toString())
                .param("direction", "down"))
        .andExpect(status().isOk());
    verify(categoryService).move(BOOK, USER, category, false);

    var member = UUID.randomUUID();
    mvc.perform(
            delete("/api/books/{bookId}/members/{memberUserId}", BOOK, member)
                .param("userId", USER.toString()))
        .andExpect(status().isOk());
    verify(memberService).remove(BOOK, USER, member);

    mvc.perform(post("/api/invites/{token}/accept", "tok").param("userId", USER.toString()))
        .andExpect(status().isOk());
    verify(memberService).accept("tok", USER);
  }

  @Test
  void badRequestsAre400NotServerErrors() throws Exception {
    // userId 없음. problem+json 본문은 advice 가 만든다(기본 처리는 400 · 빈 본문이라 advice 가 빠지면 여기서 실패).
    mvc.perform(get("/api/books"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    // UUID 형식 오류
    mvc.perform(get("/api/books/{bookId}", "not-a-uuid").param("userId", USER.toString()))
        .andExpect(status().isBadRequest());
    // 기간 누락 · 날짜 형식 오류
    mvc.perform(
            get("/api/books/{bookId}/entries", BOOK)
                .param("userId", USER.toString())
                .param("from", "2026-11-01"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get("/api/books/{bookId}/entries", BOOK)
                .param("userId", USER.toString())
                .param("from", "2026/11/01")
                .param("to", "2026-11-30"))
        .andExpect(status().isBadRequest());
    // 모르는 enum · 깨진 JSON
    mvc.perform(
            post("/api/books/{bookId}/entries", BOOK)
                .param("userId", USER.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"REFUND\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            post("/api/books", BOOK)
                .param("userId", USER.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{"))
        .andExpect(status().isBadRequest());
    verifyNoInteractions(bookService, entryService);
  }

  @Test
  void directionAcceptsOnlyUpOrDown() {
    assertThat(Direction.isUp("up")).isTrue();
    assertThat(Direction.isUp("down")).isFalse();
    assertThatThrownBy(() -> Direction.isUp("left")).isInstanceOf(BlueskyException.class);
  }

  static Entry entry(UUID asset, UUID category) {
    return new Entry(
        UUID.randomUUID(),
        BOOK,
        EntryType.EXPENSE,
        LocalDate.of(2026, 11, 3),
        LocalTime.of(12, 30),
        new BigDecimal("12000"),
        null,
        asset,
        null,
        category,
        "점심",
        null,
        USER,
        Instant.now());
  }
}
