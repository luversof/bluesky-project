package net.luversof.api.bookkeeping.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
import net.luversof.api.bookkeeping.constant.EntryType;
import net.luversof.api.bookkeeping.constant.RecurringCycle;
import net.luversof.api.bookkeeping.domain.BookRecurring;
import net.luversof.api.bookkeeping.service.AssetService;
import net.luversof.api.bookkeeping.service.AssetService.CardBill;
import net.luversof.api.bookkeeping.service.BudgetService;
import net.luversof.api.bookkeeping.service.BudgetService.BudgetCommand;
import net.luversof.api.bookkeeping.service.EntryCommand;
import net.luversof.api.bookkeeping.service.EntryService;
import net.luversof.api.bookkeeping.service.FavoriteService;
import net.luversof.api.bookkeeping.service.FxRateService;
import net.luversof.api.bookkeeping.service.FxRateService.FxRateCommand;
import net.luversof.api.bookkeeping.service.RecurringService;
import net.luversof.api.bookkeeping.service.RecurringService.Due;
import net.luversof.api.bookkeeping.service.RecurringService.RecurringCommand;

/** 2차 경로의 바인딩: JSON 본문(할부 · 월 · 주기) · 날짜 / 월 쿼리 · 응답 모양. 서비스는 가짜. */
class SecondScopeBindingTest {

  static final UUID USER = ControllerBindingTest.USER;
  static final UUID BOOK = ControllerBindingTest.BOOK;

  EntryService entryService = mock(EntryService.class);
  AssetService assetService = mock(AssetService.class);
  BudgetService budgetService = mock(BudgetService.class);
  FavoriteService favoriteService = mock(FavoriteService.class);
  RecurringService recurringService = mock(RecurringService.class);
  FxRateService fxRateService = mock(FxRateService.class);
  MockMvc mvc;

  @BeforeEach
  void setUp() {
    var properties = new CoreBaseProperties();
    properties.setLogExcludedExceptions(List.of());
    var context = new StaticApplicationContext();
    context.getBeanFactory().registerSingleton("coreBaseProperties", properties);
    context.refresh();
    context
        .getBeanFactory()
        .registerSingleton("messageSourceAccessor", new MessageSourceAccessor(context));
    ApplicationContextUtil.setApplicationContext(context);
    mvc =
        MockMvcBuilders.standaloneSetup(
                new EntryController(entryService),
                new AssetController(assetService),
                new BudgetController(budgetService),
                new FavoriteController(favoriteService),
                new RecurringController(recurringService),
                new FxRateController(fxRateService))
            .setControllerAdvice(new BookkeepingRequestErrorHandler())
            .build();
  }

  @AfterEach
  void tearDown() {
    ApplicationContextUtil.setApplicationContext(null);
  }

  /** EntryCommand 에 일시불 생성자를 더했다 - JSON 은 정식 생성자로 읽혀 할부가 살아야 한다. */
  @Test
  void entryInstallmentBindsThroughCanonicalConstructor() throws Exception {
    when(entryService.create(eq(BOOK), eq(USER), any()))
        .thenReturn(ControllerBindingTest.entry(UUID.randomUUID(), UUID.randomUUID()));
    mvc.perform(
            post("/api/books/{bookId}/entries", BOOK)
                .param("userId", USER.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"type":"EXPENSE","entryDate":"2026-11-03","amount":300000,
                     "assetId":"%s","categoryId":"%s","installmentMonths":3}
                    """
                        .formatted(UUID.randomUUID(), UUID.randomUUID())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.type").value("EXPENSE"));
    var captor = ArgumentCaptor.forClass(EntryCommand.class);
    verify(entryService).create(eq(BOOK), eq(USER), captor.capture());
    assertThat(captor.getValue().installmentMonths()).isEqualTo(3);
  }

  @Test
  void importTakesJsonArray() throws Exception {
    when(entryService.importAll(eq(BOOK), eq(USER), anyList())).thenReturn(2);
    mvc.perform(
            post("/api/books/{bookId}/entries/import", BOOK)
                .param("userId", USER.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    [{"type":"INCOME","entryDate":"2026-11-01","amount":1},
                     {"type":"EXPENSE","entryDate":"2026-11-02","amount":2,"installmentMonths":2}]
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.count").value(2));
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<EntryCommand>> captor = ArgumentCaptor.forClass(List.class);
    verify(entryService).importAll(eq(BOOK), eq(USER), captor.capture());
    assertThat(captor.getValue())
        .extracting(EntryCommand::installmentMonths)
        .containsExactly(null, 2);
  }

  @Test
  void cardBillsTakeYearMonth() throws Exception {
    var card = UUID.randomUUID();
    when(assetService.cardBills(BOOK, USER, YearMonth.of(2026, 11)))
        .thenReturn(
            List.of(
                new CardBill(
                    card,
                    YearMonth.of(2026, 11),
                    new BigDecimal("5000"),
                    new BigDecimal("33334"),
                    new BigDecimal("38334"))));
    mvc.perform(
            get("/api/books/{bookId}/assets/card-bills", BOOK)
                .param("userId", USER.toString())
                .param("month", "2026-11"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].assetId").value(card.toString()))
        .andExpect(jsonPath("$[0].month").value("2026-11"))
        .andExpect(jsonPath("$[0].total").value(38334));
    mvc.perform(
            get("/api/books/{bookId}/assets/card-bills", BOOK)
                .param("userId", USER.toString())
                .param("month", "11월"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void budgetAndFxBodies() throws Exception {
    when(budgetService.set(eq(BOOK), eq(USER), any())).thenReturn(List.of());
    mvc.perform(
            put("/api/books/{bookId}/budgets", BOOK)
                .param("userId", USER.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryId\":null,\"amount\":500000}"))
        .andExpect(status().isOk());
    var budget = ArgumentCaptor.forClass(BudgetCommand.class);
    verify(budgetService).set(eq(BOOK), eq(USER), budget.capture());
    assertThat(budget.getValue().amount()).isEqualByComparingTo("500000");

    when(fxRateService.set(eq(BOOK), eq(USER), any())).thenReturn(List.of());
    mvc.perform(
            put("/api/books/{bookId}/fx-rates", BOOK)
                .param("userId", USER.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"month\":\"2026-11\",\"currency\":\"USD\",\"rate\":1380.12345678}"))
        .andExpect(status().isOk());
    var fx = ArgumentCaptor.forClass(FxRateCommand.class);
    verify(fxRateService).set(eq(BOOK), eq(USER), fx.capture());
    assertThat(fx.getValue().month()).isEqualTo(YearMonth.of(2026, 11));
    assertThat(fx.getValue().rate()).isEqualByComparingTo("1380.12345678");

    when(fxRateService.list(BOOK, USER, YearMonth.of(2026, 11))).thenReturn(List.of());
    mvc.perform(
            get("/api/books/{bookId}/fx-rates", BOOK)
                .param("userId", USER.toString())
                .param("month", "2026-11"))
        .andExpect(status().isOk());
  }

  @Test
  void recurringBodyDueAndConfirmDates() throws Exception {
    var r =
        new BookRecurring(
            UUID.randomUUID(),
            BOOK,
            EntryType.EXPENSE,
            new BigDecimal("500000"),
            null,
            UUID.randomUUID(),
            null,
            UUID.randomUUID(),
            "월세",
            null,
            RecurringCycle.MONTHLY,
            31,
            null,
            null,
            LocalDate.of(2026, 1, 31),
            null,
            USER,
            Instant.parse("2026-11-03T00:00:00Z"));
    when(recurringService.create(eq(BOOK), eq(USER), any())).thenReturn(r);
    mvc.perform(
            post("/api/books/{bookId}/recurrings", BOOK)
                .param("userId", USER.toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"type":"EXPENSE","amount":500000,"assetId":"%s","categoryId":"%s",
                     "cycle":"MONTHLY","dayOfMonth":31,"dayOfWeek":6,"monthOfYear":1,
                     "startDate":"2026-01-31","endDate":null}
                    """
                        .formatted(UUID.randomUUID(), UUID.randomUUID())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cycle").value("MONTHLY"))
        .andExpect(jsonPath("$.startDate").value("2026-01-31"));
    var captor = ArgumentCaptor.forClass(RecurringCommand.class);
    verify(recurringService).create(eq(BOOK), eq(USER), captor.capture());
    assertThat(captor.getValue().cycle()).isEqualTo(RecurringCycle.MONTHLY);
    assertThat(captor.getValue().startDate()).isEqualTo(LocalDate.of(2026, 1, 31));

    when(recurringService.due(BOOK, USER, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30)))
        .thenReturn(List.of(new Due(r, LocalDate.of(2026, 11, 30))));
    mvc.perform(
            get("/api/books/{bookId}/recurrings/due", BOOK)
                .param("userId", USER.toString())
                .param("from", "2026-11-01")
                .param("to", "2026-11-30"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].date").value("2026-11-30"))
        .andExpect(jsonPath("$[0].recurring.dayOfMonth").value(31));

    when(recurringService.confirm(BOOK, USER, r.id(), LocalDate.of(2026, 11, 30)))
        .thenReturn(ControllerBindingTest.entry(UUID.randomUUID(), UUID.randomUUID()));
    mvc.perform(
            post("/api/books/{bookId}/recurrings/{id}/confirm", BOOK, r.id())
                .param("userId", USER.toString())
                .param("date", "2026-11-30"))
        .andExpect(status().isOk());
    mvc.perform(
            post("/api/books/{bookId}/recurrings/{id}/confirm", BOOK, r.id())
                .param("userId", USER.toString())
                .param("date", "11/30/26"))
        .andExpect(status().isBadRequest());
  }
}
