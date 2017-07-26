package net.luversof.bookkeeping;

import java.util.List;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.EntryGroup;
import net.luversof.bookkeeping.service.BookkeepingService;
import net.luversof.bookkeeping.service.EntryGroupService;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class EntryGroupTest extends GeneralTest {
	
	@Autowired
	private EntryGroupService entryGroupService;
	
	@Autowired
	private BookkeepingService bookkeepingService;
	
	private Bookkeeping bookkeeping;
	
	static final UUID TEST_USER_ID = UUID.fromString("1");
	
	@Before
	public void before() {
		bookkeeping = bookkeepingService.findByUserId(TEST_USER_ID).get(0);
	}
	
	@Test
	public void initialDataSave() {
		List<EntryGroup> result = entryGroupService.initialDataSave(bookkeeping);
		log.debug("defaultSave : {}", result);
	}
	
	@Test
	public void findEntryGroupList() {
		List<EntryGroup> entryGroupList = entryGroupService.findByBookkeepingId(bookkeeping.getId());
		log.debug("entryGroupList : {}", entryGroupList);
	}

}
