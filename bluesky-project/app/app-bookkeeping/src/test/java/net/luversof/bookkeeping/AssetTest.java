package net.luversof.bookkeeping;

import java.util.Date;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.domain.AssetGroup;
import net.luversof.bookkeeping.domain.AssetType;
import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.domain.QEntry;
import net.luversof.bookkeeping.repository.EntryRepository;
import net.luversof.bookkeeping.service.AssetGroupService;
import net.luversof.bookkeeping.service.AssetService;
import net.luversof.data.jpa.datasource.DataSourceContextHolder;
import net.luversof.data.jpa.datasource.DataSourceType;

import org.joda.time.DateTime;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

import com.mysema.query.types.Predicate;

@Slf4j
public class AssetTest extends GeneralTest {
	
	@Autowired
	private AssetService assetService;

	@Autowired
	private AssetGroupService assetGroupService;
	
	@Test
	@Ignore
	public void assetGroup추가() {
		
		AssetGroup assetGroup = new AssetGroup();
		assetGroup.setAssetType(AssetType.CARD);
		assetGroup.setName("test");
		assetGroup.setUsername("bluesky");
		AssetGroup resultAssetGroup = assetGroupService.save(assetGroup);
		log.debug("resultAssetGroup : {}", resultAssetGroup);
	}
	@Test
	@Ignore
	public void test() {
		
		AssetGroup assetGroup = assetGroupService.findOne(1);
		
		for (int i = 1 ; i < 100; i++) {
			
			Asset asset = new Asset();
			asset.setName("test" + i);
			asset.setUsername("bluesky");
			asset.setAmount(100);
			asset.setAssetGroup(assetGroup);
			asset.setEnable(true);
			Asset resultAsset = assetService.save(asset);
			log.debug("result : {}", resultAsset);
		}
	}
	
	@Test
	public void test2() {
		List<Asset> assetList = assetService.findByUsername("bluesky");
		log.debug("assetList : {}", assetList);
	}
	
	@Autowired
	private EntryRepository entryRepository;

	
	/**
	 * querydsl로  page = 0 && order by date desc 로 검색
	 */
	@Test
	public void querydsl을_이용한_페이징() {
		DataSourceContextHolder.setDataSourceType(DataSourceType.BOOKKEEPING);
		QEntry qEntry = QEntry.entry;
		Predicate predicate = qEntry.asset.id.eq((long) 1);
		Pageable pageable = new PageRequest(0, 10, new Sort(Direction.DESC, qEntry.date.getMetadata().getName()));
		Page<Entry> assetPage = entryRepository.findAll(predicate, pageable);
		log.debug("list : {}", assetPage);
	}
	
	/**
	 * spring data jpa로 한 경우
	 * 개인적으론 querydsl을 쓸 정도로 명시해야할 이유는 없다고 생각함
	 * 다만 컬럼 명 명시까진 querydsl로 제한을 거는건 좋다고 생각함
	 */
	@Test
	public void spring_data_jpa를_이용한_페이징() {
		DataSourceContextHolder.setDataSourceType(DataSourceType.BOOKKEEPING);
		Pageable pageable = new PageRequest(0, 10, new Sort(Direction.DESC, QEntry.entry.date.getMetadata().getName()));
		Page<Entry> entryPage = entryRepository.findByAssetUsername("bluesky", pageable);
		log.debug("list : {}", entryPage);
	}
	
	/**
	 * 일정 기간으로 구간 검색하기
	 */
	@Test
	public void test5() {
		DataSourceContextHolder.setDataSourceType(DataSourceType.BOOKKEEPING);
		Date startDate = new Date(2013-1900, 9, 4);
		Date endDate = new Date(2013-1900, 9, 20);
		DateTime startDate2 = new DateTime(1913, 10, 4, 0, 0);
		DateTime endDate2 = new DateTime(2013, 10, 20, 0, 0);
		List<Entry> entryList = entryRepository.findByAssetUsernameAndDateBetween("bluesky", startDate2, endDate2);
		log.debug("list : {}", entryList);
	}
}
