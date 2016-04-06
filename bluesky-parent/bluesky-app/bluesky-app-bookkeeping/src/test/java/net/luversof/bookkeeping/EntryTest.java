package net.luversof.bookkeeping;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.bookkeeping.service.EntryService;

@Slf4j
public class EntryTest extends GeneralTest {

	@Autowired
	private EntryService entryService;
	
	
	@Test
	public void test () {
		log.debug("result : {}", entryService.findByBookkeepingId(1));
	}
}
