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
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.domain.web.EntryRequestParam;
import net.luversof.bookkeeping.service.EntryService;
import net.luversof.security.core.userdetails.BlueskyUser;

@RestController
@BlueskyPreAuthorize
@RequestMapping(value = "/api/bookkeeping/entry", produces = MediaType.APPLICATION_JSON_VALUE)
public class EntryController {
	
	@Autowired
	private EntryService entryService;
	
	@PostMapping
	public Entry createUserEntry(@RequestBody @Validated(Entry.Create.class) Entry entry, BlueskyUser blueskyUser) {
		entry.setBookkeeping(new Bookkeeping());
		entry.getBookkeeping().setUserId(blueskyUser.getId());
		return entryService.createUserEntry(entry);
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
	public List<Entry> searchUserEntry(@Validated(EntryRequestParam.Search.class) EntryRequestParam entryRequestParam, BlueskyUser blueskyUser) {
		entryRequestParam.setBookkeeping(new Bookkeeping());
		entryRequestParam.getBookkeeping().setUserId(blueskyUser.getId());
		return entryService.searchUserEntry(entryRequestParam);
	}
	
	@PutMapping
	public Entry updateUserEntry(@RequestBody @Validated(Entry.Update.class) Entry entry, BlueskyUser blueskyUser) {
		entry.setBookkeeping(new Bookkeeping());
		entry.getBookkeeping().setUserId(blueskyUser.getId());
		return entryService.updateUserEntry(entry);
	}
	
	@DeleteMapping
	public void deleteUserEntry(@Validated(Entry.Delete.class) Entry entry, BlueskyUser blueskyUser) {
		entry.setBookkeeping(new Bookkeeping());
		entry.getBookkeeping().setUserId(blueskyUser.getId());
		entryService.deleteUserEntry(entry);
	}
}
