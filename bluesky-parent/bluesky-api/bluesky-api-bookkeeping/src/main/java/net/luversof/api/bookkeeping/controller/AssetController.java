package net.luversof.api.bookkeeping.controller;

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

  @GetMapping
  public List<AssetBalance> list(@PathVariable UUID bookId, @RequestParam UUID userId) {
    return assetService.list(bookId, userId);
  }

  @PostMapping
  public Asset create(
      @PathVariable UUID bookId, @RequestParam UUID userId, @RequestBody AssetCommand command) {
    return assetService.create(bookId, userId, command);
  }

  @PutMapping("/{assetId}")
  public Asset update(
      @PathVariable UUID bookId,
      @PathVariable UUID assetId,
      @RequestParam UUID userId,
      @RequestBody AssetCommand command) {
    return assetService.update(bookId, userId, assetId, command);
  }

  @DeleteMapping("/{assetId}")
  public DeleteResult delete(
      @PathVariable UUID bookId, @PathVariable UUID assetId, @RequestParam UUID userId) {
    return new DeleteResult(assetService.delete(bookId, userId, assetId));
  }

  @PostMapping("/{assetId}/move")
  public void move(
      @PathVariable UUID bookId,
      @PathVariable UUID assetId,
      @RequestParam UUID userId,
      @RequestParam String direction) {
    assetService.move(bookId, userId, assetId, Direction.isUp(direction));
  }
}
