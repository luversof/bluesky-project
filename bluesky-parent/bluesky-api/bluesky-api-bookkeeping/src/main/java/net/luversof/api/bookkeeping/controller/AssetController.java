package net.luversof.api.bookkeeping.controller;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.service.AssetService;
import net.luversof.api.bookkeeping.service.AssetService.AssetBalance;
import net.luversof.api.bookkeeping.service.AssetService.AssetCommand;

/** 자산(잔액 포함 목록). */
@RestController
@RequestMapping(value = "/api/books/{bookId}/assets", produces = MediaType.APPLICATION_JSON_VALUE)
public class AssetController {

  private final AssetService assetService;

  public AssetController(AssetService assetService) {
    this.assetService = assetService;
  }

  /**
   * 가계부의 자산 목록(잔액 포함). 숨긴 자산도 들어 있다.
   *
   * @param bookId 가계부 id
   * @param userId 요청한 사용자
   * @return 자산과 잔액
   */
  @GetMapping
  public List<AssetBalance> list(@PathVariable UUID bookId, @RequestParam UUID userId) {
    return assetService.list(bookId, userId);
  }

  /** 카드별 결제 예정액. month 는 "2026-11"(결제 달). */
  @GetMapping("/card-bills")
  public List<AssetService.CardBill> cardBills(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestParam YearMonth month) {
    return assetService.cardBills(bookId, userId, month);
  }

  /**
   * 자산을 만든다.
   *
   * @param bookId 가계부 id
   * @param userId 요청한 사용자
   * @param command 이름 · 종류 · 통화 · 시작 잔액
   * @return 만든 자산
   */
  @PostMapping
  public Asset create(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestBody AssetCommand command) {
    return assetService.create(bookId, userId, command);
  }

  /**
   * 자산을 고친다. 내역이 있으면 통화는 바꿀 수 없다.
   *
   * @param bookId 가계부 id
   * @param assetId 자산 id
   * @param userId 요청한 사용자
   * @param command 바꿀 값
   * @return 고친 자산
   */
  @PutMapping("/{assetId}")
  public Asset update(
      @PathVariable UUID bookId,
      @PathVariable UUID assetId,
      @RequestParam UUID userId,
      @RequestBody AssetCommand command) {
    return assetService.update(bookId, userId, assetId, command);
  }

  /**
   * 자산을 지운다. 내역이 있으면 지우지 않고 숨긴다.
   *
   * @param bookId 가계부 id
   * @param assetId 자산 id
   * @param userId 요청한 사용자
   * @return 지웠으면 deleted=true, 숨기기만 했으면 false
   */
  @DeleteMapping("/{assetId}")
  public DeleteResult delete(
      @PathVariable UUID bookId, @PathVariable UUID assetId, @RequestParam UUID userId) {
    return new DeleteResult(assetService.delete(bookId, userId, assetId));
  }

  /**
   * 자산 순서를 같은 종류 안에서 한 칸 옮긴다. 끝이면 그대로 둔다.
   *
   * @param bookId 가계부 id
   * @param assetId 자산 id
   * @param userId 요청한 사용자
   * @param direction up 또는 down
   */
  @PostMapping("/{assetId}/move")
  public void move(
      @PathVariable UUID bookId,
      @PathVariable UUID assetId,
      @RequestParam UUID userId,
      @RequestParam String direction) {
    assetService.move(bookId, userId, assetId, Direction.isUp(direction));
  }
}
