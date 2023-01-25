package net.luversof.bookkeeping;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.bookkeeping.constant.EntryGroupInitialData;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.EntryGroup;
import net.luversof.bookkeeping.service.BasicBookkeepingService;
import net.luversof.bookkeeping.service.CompositeEntryGroupService;

@Slf4j
class EntryGroupTest implements GeneralTest {
	
	@Autowired
	private BasicBookkeepingService bookkeepingService;
	
	@Autowired
	private CompositeEntryGroupService entryGroupService;
	
	private Bookkeeping bookkeeping;

	
	@BeforeEach
	public void before() {
		bookkeeping = bookkeepingService.findByUserId(BookkeepingTestConstant.USER_ID).stream().findFirst().get();
	}

	@Test
	void entryGroupInitialDataName() {
		Arrays.stream(EntryGroupInitialData.values()).forEach(x -> log.debug("{} name : {}", x, x.getName()));
	}
	@Test
	void initialDataSave() {
		List<EntryGroup> result = entryGroupService.createInitialData(bookkeeping.getBookkeepingId());
		log.debug("defaultSave : {}", result);
	}
	
	@Test
	void findEntryGroupList() {
		List<EntryGroup> entryGroupList = entryGroupService.findByBookkeepingId(bookkeeping.getBookkeepingId());
		log.debug("entryGroupList : {}", entryGroupList);
	}

}
