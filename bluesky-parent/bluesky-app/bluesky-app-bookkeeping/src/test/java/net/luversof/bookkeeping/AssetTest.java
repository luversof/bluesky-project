package net.luversof.bookkeeping;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;
import net.luversof.GeneralTest;
import net.luversof.bookkeeping.constant.AssetInitialData;
import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.repository.EntryRepository;
import net.luversof.bookkeeping.service.AssetService;
import net.luversof.bookkeeping.service.BookkeepingService;

@Slf4j
public class AssetTest extends GeneralTest {
	
	@Autowired
	private AssetService assetService;
	
	@Autowired
	private BookkeepingService bookkeepingService;

	static final UUID TEST_USER_ID = UUID.fromString("35929103-da22-49e7-9d76-214bb081593f");

	@Test
	public void assetInitialDataName() {
		log.debug("defaultAsset name : {}", AssetInitialData.WALLET.getName());
	}

	@Test
	public void initialDataSave() {
		Bookkeeping bookkeeping = new Bookkeeping();
		bookkeeping.setUserId(TEST_USER_ID);
		Bookkeeping userBookkeeping = bookkeepingService.getUserBookkeeping(bookkeeping).get();
		List<Asset> result = assetService.initialDataSave(userBookkeeping);
		log.debug("defaultAsset : {}", result);
	}
	
	
	@SuppressWarnings("unused")
	@Autowired
	private EntryRepository entryRepository;

	
//	/**
//	 * querydsl로  page = 0 && order by date desc 로 검색
//	 */
//	@Test
//	public void querydsl을_이용한_페이징() {
//		DataSourceContextHolder.setDataSourceType(DataSourceType.BOOKKEEPING);
//		QEntry qEntry = QEntry.entry;
//		Predicate predicate = qEntry.asset.id.eq((long) 1);
//		Pageable pageable = new PageRequest(0, 10, new Sort(Direction.DESC, qEntry.date.getMetadata().getName()));
//		Page<Entry> assetPage = entryRepository.findAll(predicate, pageable);
//		log.debug("list : {}", assetPage);
//	}
	
//	/**
//	 * spring data jpa로 한 경우
//	 * 개인적으론 querydsl을 쓸 정도로 명시해야할 이유는 없다고 생각함
//	 * 다만 컬럼 명 명시까진 querydsl로 제한을 거는건 좋다고 생각함
//	 */
//	@Test
//	public void spring_data_jpa를_이용한_페이징() {
//		DataSourceContextHolder.setDataSourceType(DataSourceType.BOOKKEEPING);
//		Pageable pageable = new PageRequest(0, 10, new Sort(Direction.DESC, QEntry.entry.date.getMetadata().getName()));
//		Page<Entry> entryPage = entryRepository.findByAssetUserId(1, pageable);
//		log.debug("list : {}", entryPage);
//	}
	
//	/**
//	 * 일정 기간으로 구간 검색하기
//	 */
//	@Test
//	public void test5() {
//		DataSourceContextHolder.setDataSourceType(DataSourceType.BOOKKEEPING);
//		Date startDate = new Date(2013-1900, 9, 4);
//		Date endDate = new Date(2013-1900, 9, 20);
//		DateTime startDate2 = new DateTime(1913, 10, 4, 0, 0);
//		DateTime endDate2 = new DateTime(2013, 10, 20, 0, 0);
//		List<Entry> entryList = entryRepository.findByAssetUserIdAndDateBetween(1, startDate2, endDate2);
//		log.debug("list : {}", entryList);
//	}
}
