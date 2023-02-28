package net.luversof.api.bookkeeping.service;

import java.util.List;
import java.util.Optional;

import net.luversof.api.bookkeeping.domain.Bookkeeping;

public interface BookkeepingService {


	/**
	 * 가계부 생성시 아래 default 데이터 생성
	 * 기본 자산 (asset)
	 * 기본 기록 그룹 (entryGroup)
	 * @param bookkeeping
	 * @return
	 */
	Bookkeeping create(Bookkeeping bookkeeping);
	
	Optional<Bookkeeping> findByBookkeepingId(String bookkeepingId);
	
	List<Bookkeeping> findByUserId(String userId);
	
	Bookkeeping update(Bookkeeping bookkeeping);

	/**
	 * 완전 삭제의 경우 관련한 데이터를 모두 삭제 처리
	 * @param bookkeeping
	 */
	void delete(Bookkeeping bookkeeping);

}
