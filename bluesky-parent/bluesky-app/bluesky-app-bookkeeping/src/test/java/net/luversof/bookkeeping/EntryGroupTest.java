package net.luversof.bookkeeping;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.bookkeeping.constant.EntryGroupInitialData;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.EntryGroup;
import net.luversof.bookkeeping.service.BookkeepingService;
import net.luversof.bookkeeping.service.EntryGroupService;

@Slf4j
public class EntryGroupTest extends GeneralTest {
	
	@Autowired
	private EntryGroupService entryGroupService;
	
	@Autowired
	private BookkeepingService bookkeepingService;
	
	private Bookkeeping bookkeeping;

	static final UUID TEST_USER_ID = UUID.fromString("35929103-da22-49e7-9d76-214bb081593f");
	
	@BeforeEach
	public void before() {
		Bookkeeping bookkeeping = new Bookkeeping();
    	bookkeeping.setUserId(TEST_USER_ID);
		bookkeeping = bookkeepingService.getUserBookkeeping(bookkeeping.getUserId()).get();
	}

	@Test
	public void entryGroupInitialDataName() {
		Arrays.stream(EntryGroupInitialData.values()).forEach(x -> log.debug("{} name : {}", x, x.getName()));
	}
	@Test
	public void initialDataSave() {
		List<EntryGroup> result = entryGroupService.initialDataSave(bookkeeping);
		log.debug("defaultSave : {}", result);
	}
	
	@Test
	public void findEntryGroupList() {
		List<EntryGroup> entryGroupList = entryGroupService.getUserEntryGroupList(TEST_USER_ID);
		log.debug("entryGroupList : {}", entryGroupList);
	}

}
