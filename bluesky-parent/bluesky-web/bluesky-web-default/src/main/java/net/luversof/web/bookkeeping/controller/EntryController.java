package net.luversof.web.bookkeeping.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;
import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.domain.web.EntryRequestParam;
import net.luversof.bookkeeping.service.CompositeBookkeepingService;
import net.luversof.bookkeeping.service.CompositeEntryService;
import net.luversof.security.core.userdetails.BlueskyUser;

@RestController
@BlueskyPreAuthorize
@RequestMapping(value = "/api/bookkeeping/entry", produces = MediaType.APPLICATION_JSON_VALUE)
public class EntryController {
	
	@Autowired
	private CompositeEntryService entryService;
	
	@Autowired
	private CompositeBookkeepingService bookkeepingService;
	
	@PostMapping
	public Entry create(@RequestBody @Validated(Entry.Create.class) Entry entry, BlueskyUser blueskyUser) {
		var bookkeepingList = bookkeepingService.findByUserId(blueskyUser.getId());
		entry.setBookkeepingId(bookkeepingList.stream().findFirst().get().getBookkeepingId());
		return entryService.create(entry);
	}
	
	/*
	 * 검색 조건은 어떤 것이 있을까?
	 * 1. 날짜
	 * 2. Asset 별
	 * 3. 입출력 별 등등
	 * 위의 다양한 경우가 있지만 1차 순위로 월별 검색으로 처리해보자.
	 * 그 다음 월별이 아닌 마감일설정 별로 검색 처리를 해보자.
	 * 검색 조건별 검색처리는 통계 정보 제공으로 넘기자.
	 * 
	 * 페이지에서 사용할 데이터를 내려줘야할까?
	 * 우선 검색 결과를 내려준 이후 처리 고민
	 */
	@GetMapping
	public List<Entry> search(@Validated(EntryRequestParam.Search.class) EntryRequestParam entryRequestParam, BlueskyUser blueskyUser) {
		var bookkeepingList = bookkeepingService.findByUserId(blueskyUser.getId());
		entryRequestParam.setBookkeepingId(bookkeepingList.stream().findFirst().get().getBookkeepingId());
		return entryService.search(entryRequestParam);
	}
	
	@PutMapping
	public Entry update(@RequestBody @Validated(Entry.Update.class) Entry entry, BlueskyUser blueskyUser) {
		var bookkeepingList = bookkeepingService.findByUserId(blueskyUser.getId());
		entry.setBookkeepingId(bookkeepingList.stream().findFirst().get().getBookkeepingId());
		return entryService.update(entry);
	}
	
	@DeleteMapping
	public void delete(@Validated(Entry.Delete.class) Entry entry, BlueskyUser blueskyUser) {
		var bookkeepingList = bookkeepingService.findByUserId(blueskyUser.getId());
		entry.setBookkeepingId(bookkeepingList.stream().findFirst().get().getBookkeepingId());
		entryService.delete(entry);
	}
}
