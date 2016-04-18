package net.luversof.bookkeeping;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.domain.EntryGroup;
import net.luversof.bookkeeping.service.BookkeepingService;
import net.luversof.bookkeeping.service.EntryGroupService;
import net.luversof.bookkeeping.service.EntryService;

@Slf4j
public class EntryTest extends GeneralTest {
	
	@Autowired
	private BookkeepingService bookkeepingService;

	@Autowired
	private EntryGroupService entryGroupService;
	
	@Autowired
	private EntryService entryService;
	
	
	@Test
	public void test () {
		log.debug("result : {}", entryService.findByBookkeepingIdAndEntryDateBetween(1, null, null));
	}
	
	// 세이브 테스트
	@Test
	public void test2() {
		Bookkeeping bookkeeping = bookkeepingService.findOne(1);
		List<EntryGroup> entryGroupList = entryGroupService.findByBookkeepingId(bookkeeping.getId());
		
		Entry entry = new Entry();
		entry.setBookkeeping(bookkeeping);
		entry.setAmount(123);
		entry.setEntryGroup(entryGroupList.get(3));
		entry.setMemo("test");
		
		entryService.create(entry);
		
		log.debug("Test : {}", entry);
	}
	
	
	@Test
	public void test3() {
		
		log.debug("result : {}", entryService.findByBookkeepingIdAndEntryDateBetween(1, null, null));
	}
}
