package net.luversof.bookkeeping.service;

import java.util.List;

import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.repository.BookkeepingRepository;
import net.luversof.opensource.jdbc.routing.DataSource;
import net.luversof.opensource.jdbc.routing.DataSourceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@DataSource(DataSourceType.BOOKKEEPING)
public class BookkeepingService {

	@Autowired
	private BookkeepingRepository bookkeepingRepository;
	
	@Autowired
	private EntryGroupService entryGroupService;
	
	@Autowired
	private AssetService assetService;
	
	/**
	 * 가계부 생성시 아래 default 데이터 생성
	 * 기본 자산 (asset)
	 * 기본 기록 그룹 (entryGroup)
	 * @param bookkeeping
	 * @return
	 */
	public Bookkeeping save(Bookkeeping bookkeeping) {
		boolean isSave = bookkeeping.getId() == 0;
		bookkeepingRepository.save(bookkeeping);
		if (isSave) {
			assetService.initialDataSave(bookkeeping);
			entryGroupService.initialDataSave(bookkeeping);
		}
		return bookkeeping;
	}
	
	@Transactional(readOnly = true)
	public Bookkeeping findOne(long id) {
		return bookkeepingRepository.findOne(id);
	}
	
	@Transactional(readOnly = true)
	public List<Bookkeeping> findByUserId(long userId) {
		return bookkeepingRepository.findByUserId(userId);
	}
	
	/**
	 * 삭제의 경우 관련한 데이터를 모두 삭제 처리를 해야할까?
	 * @param bookkeeping
	 */
	public void delete(Bookkeeping bookkeeping) {
		entryGroupService.deleteBybookkeepingId(bookkeeping.getId());
		assetService.deleteBybookkeepingId(bookkeeping.getId());
		bookkeepingRepository.delete(bookkeeping);
	}
}
