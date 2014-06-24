package net.luversof.bookkeeping;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import net.luversof.bookkeeping.domain.EntryGroup;
import net.luversof.bookkeeping.service.BookkeepingService;
import net.luversof.bookkeeping.service.EntryGroupService;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class EntryGroupTest extends GeneralTest {
	
	@Autowired
	private EntryGroupService entryGroupService;
	
	@Autowired
	private BookkeepingService bookkeepingService;
	
	@Test
	public void initialDataSave() {
		List<EntryGroup> result = entryGroupService.initialDataSave(bookkeepingService.findByUserId(TEST_USER_ID).get(0));
		log.debug("defaultSave : {}", result);
	}

}
