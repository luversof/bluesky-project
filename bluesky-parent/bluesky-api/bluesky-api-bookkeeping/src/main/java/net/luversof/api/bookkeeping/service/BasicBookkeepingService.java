//package net.luversof.api.bookkeeping.service;
//
//
//import java.util.List;
//import java.util.Optional;
//import java.util.UUID;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//import org.springframework.util.StringUtils;
//
//import io.github.luversof.boot.exception.BlueskyException;
//import net.luversof.api.bookkeeping.base.domain.Bookkeeping;
//import net.luversof.api.bookkeeping.base.repository.mariadb.BookkeepingRepository;
//import net.luversof.api.bookkeeping.constant.BookkeepingErrorCode;
//
//@Service
//public class BasicBookkeepingService implements BookkeepingService {
//
//	@Autowired
//	private BookkeepingRepository bookkeepingRepository;
//
//	@Override
//	public Bookkeeping create(Bookkeeping bookkeeping) {
//		if (bookkeeping.getUserId() == null) {
//			throw new BlueskyException(BookkeepingErrorCode.NOT_EXIST_USER_ID);
//		}
//		return bookkeepingRepository.save(bookkeeping);
//	}
//
//	@Override
//	public Optional<Bookkeeping> findById(UUID bookkeepingId) {
//		return bookkeepingRepository.findById(bookkeepingId);
//	}
//	
//
//	@Override
//	public List<Bookkeeping> findByUserId(UUID userId) {
//		return bookkeepingRepository.findByUserId(userId);
//	}
//
//	@Override
//	public Bookkeeping update(Bookkeeping bookkeeping) {
//		Bookkeeping targetBookkeeping = findById(bookkeeping.getId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
//		if (bookkeeping.getBaseDate() > 0) {
//			targetBookkeeping.setBaseDate(bookkeeping.getBaseDate());
//		}
//
//		if (StringUtils.hasText(bookkeeping.getName())) {
//			targetBookkeeping.setName(bookkeeping.getName());
//		}
//
//		return bookkeepingRepository.save(targetBookkeeping);
//	}
//
//	@Override
//	public void delete(Bookkeeping bookkeeping) {
//		Bookkeeping targetBookkeeping = bookkeepingRepository.findById(bookkeeping.getId()).orElseThrow(() -> new BlueskyException(BookkeepingErrorCode.NOT_EXIST_BOOKKEEPING));
//		bookkeepingRepository.delete(targetBookkeeping);
//	}
//
//}
