package net.luversof.api.stock.web.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.stock.domain.Account;
import net.luversof.api.stock.service.AccountService;

@RestController
@RequestMapping("/api/account")
public class AccountController {

  @Autowired private AccountService accountService;

  /**
   * 계좌를 만든다.
   *
   * @param account 만들 계좌(사용자 · 이름 · 기준통화)
   * @return 만든 계좌
   */
  @PostMapping
  public Account createAccount(@RequestBody Account account) {
    return accountService.createAccount(account);
  }

  /**
   * 계좌 한 건.
   *
   * @param id 계좌 id
   * @return 계좌, 없으면 빈 값
   */
  @GetMapping("/{id}")
  public Optional<Account> getAccountById(@PathVariable UUID id) {
    return accountService.findById(id);
  }

  /**
   * 사용자의 계좌 목록.
   *
   * @param userId 사용자 id
   * @return 계좌 목록
   */
  @GetMapping("/search/findByUserId/{userId}")
  public List<Account> getAccountsByUserId(@PathVariable UUID userId) {
    return accountService.findByUserId(userId);
  }
}
