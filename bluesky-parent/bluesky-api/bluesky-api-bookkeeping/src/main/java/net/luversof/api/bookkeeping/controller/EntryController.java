package net.luversof.api.bookkeeping.controller;

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

import net.luversof.api.bookkeeping.domain.Entry;
import net.luversof.api.bookkeeping.domain.web.EntryRequestParam;
import net.luversof.api.bookkeeping.service.CompositeEntryService;

@RestController
@RequestMapping(value = "/api/bookkeeping/entry", produces = MediaType.APPLICATION_JSON_VALUE)
public class EntryController {
	
	@Autowired
	private CompositeEntryService entryService;
	
	@PostMapping
	public Entry create(@RequestBody @Validated(Entry.Create.class) Entry entry) {
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
	public List<Entry> search(@Validated(EntryRequestParam.Search.class) EntryRequestParam entryRequestParam) {
		return entryService.search(entryRequestParam);
	}
	
	@PutMapping
	public Entry update(@RequestBody @Validated(Entry.Update.class) Entry entry) {
		return entryService.update(entry);
	}
	
	@DeleteMapping
	public void delete(@Validated(Entry.Delete.class) Entry entry) {
		entryService.delete(entry);
	}
}
