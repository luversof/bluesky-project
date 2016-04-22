package net.luversof.bookkeeping.service;

import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.EntrySearchInfo;

@Service
public class EntrySearchInfoService {
	
	@Autowired
	private BookkeepingService bookkeepingService;
	
	
	public EntrySearchInfo getEntrySearchInfo(long bookkeepingId, LocalDate targetDate) {
		
		Bookkeeping targetBookkeeping = bookkeepingService.findOne(bookkeepingId);
		if (targetBookkeeping == null) {
			
		}
		return null;
	}

}
