//package net.luversof.api.bookkeeping.composite;
//
//import java.math.BigDecimal;
//import java.time.ZonedDateTime;
//import java.util.ArrayList;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//
//import lombok.extern.slf4j.Slf4j;
//import net.luversof.GeneralTest;
//import net.luversof.api.bookkeeping.composite.service.TransactionRecordCompositeService;
//import net.luversof.api.bookkeeping.constant.TestConstant;
//import net.luversof.api.bookkeeping.domain.Entry;
//import net.luversof.api.bookkeeping.repository.mariadb.AssetRepository;
//import net.luversof.api.bookkeeping.repository.mariadb.BookkeepingRepository;
//import net.luversof.api.bookkeeping.repository.mariadb.EntryRepository;
//import net.luversof.api.bookkeeping.repository.mariadb.EntryTypeRepository;
//
//@Slf4j
//class EntryCompositeServiceTest  implements GeneralTest {
//	
//	@Autowired
//	private BookkeepingRepository bookkeepingRepository;
//	
//	@Autowired
//	private AssetRepository assetRepository;
//	
//	@Autowired
//	private EntryTypeRepository transactionTypeRepository;
//	
//	@Autowired
//	private EntryRepository transactionRecordRepository;
//	
//	@Autowired
//	private TransactionRecordCompositeService entryCompositeService;
//
//	@Test
//	void serviceSaveTest() {
//		var bookkeeping = bookkeepingRepository.findByUserId(TestConstant.USER_ID).get(0);
//		var account = assetRepository.findByBookkeepingId(bookkeeping.getId()).get(0);
//		var transactionType = transactionTypeRepository.findByBookkeepingId(bookkeeping.getId()).get(0);
//		
//		var entryList = new ArrayList<Entry>();
//		{
//			var entry = new Entry();
//			entry.setAssetId(account.getId());
//			entry.setCredit(new BigDecimal("123123"));
//			entry.setTransactionTypeId(transactionType.getId());
//			entry.setTransactionDate(ZonedDateTime.now());
//			entryList.add(entry);
//		}
//		{
//			var entry = new Entry();
//			entry.setAssetId(account.getId());
//			entry.setTransactionTypeId(transactionType.getId());
//			entry.setDebit(new BigDecimal("123123"));
//			entry.setTransactionDate(ZonedDateTime.now());
//			entryList.add(entry);
//		}
//		
//		
//		entryCompositeService.save(entryList);
//	}
//
//}
