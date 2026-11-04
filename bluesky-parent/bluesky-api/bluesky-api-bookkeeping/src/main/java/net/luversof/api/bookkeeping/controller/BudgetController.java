package net.luversof.api.bookkeeping.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.bookkeeping.domain.Budget;
import net.luversof.api.bookkeeping.service.BudgetService;
import net.luversof.api.bookkeeping.service.BudgetService.BudgetCommand;

/** 월 예산(전체 · 지출 대분류별, 기준통화). */
@RestController
@RequestMapping(value = "/api/books/{bookId}/budgets", produces = MediaType.APPLICATION_JSON_VALUE)
public class BudgetController {

  private final BudgetService budgetService;

  public BudgetController(BudgetService budgetService) {
    this.budgetService = budgetService;
  }

  @GetMapping
  public List<Budget> list(@PathVariable UUID bookId, @RequestParam UUID userId) {
    return budgetService.list(bookId, userId);
  }

  /** categoryId 를 비우면 전체 예산. amount 를 비우거나 0 이면 지운다. 바뀐 뒤 목록. */
  @PutMapping
  public List<Budget> set(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestBody BudgetCommand command) {
    return budgetService.set(bookId, userId, command);
  }
}
