package net.luversof.web.bookkeeping.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.domain.EntrySearchInfo;
import net.luversof.bookkeeping.domain.EntrySearchInfo.SelectEntryList;
import net.luversof.bookkeeping.service.EntryService;
import net.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;
import net.luversof.security.core.userdetails.BlueskyUser;

//@RestController
@BlueskyPreAuthorize
@RequestMapping(value = "/bookkeeping/{bookkeeping.id}/entry", produces = MediaType.APPLICATION_JSON_VALUE)
public class EntryController {
	
	@Autowired
	private EntryService entryService;
	
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
	@PostAuthorize("(returnObject == null or returnObject.size() == 0) or returnObject.get(0).bookkeeping.userId == authentication.principal.id")
	@GetMapping
	public List<Entry> getEntryList(@Validated(SelectEntryList.class) EntrySearchInfo entrySearchInfo) {
		return entryService.findByEntrySearchInfo(entrySearchInfo);
	}
	
	@PostMapping
	public Entry createEntry(@RequestBody @Validated(Entry.Create.class) Entry entry, Authentication authentication) {
		entry.getBookkeeping().setUserId(((BlueskyUser) authentication.getPrincipal()).getId());
		return entryService.create(entry);
	}

	@PutMapping(value = "/{id}")
	public Entry modify(@RequestBody @Validated(Entry.Update.class) Entry entry, Authentication authentication) {
		entry.getBookkeeping().setUserId(((BlueskyUser) authentication.getPrincipal()).getId());
		return entryService.update(entry);
	}
	
	@DeleteMapping(value = "/{id}")
	public void delete(@Validated(Entry.Delete.class) Entry entry, Authentication authentication) {
		entry.getBookkeeping().setUserId(((BlueskyUser) authentication.getPrincipal()).getId());
		entryService.delete(entry);
	}
}
