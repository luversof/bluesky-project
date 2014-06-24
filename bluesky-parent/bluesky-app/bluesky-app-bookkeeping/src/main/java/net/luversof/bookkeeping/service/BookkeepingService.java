package net.luversof.bookkeeping.service;

import java.util.List;

import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.repository.BookkeepingRepository;
import net.luversof.jdbc.datasource.DataSource;
import net.luversof.jdbc.datasource.DataSourceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@DataSource(DataSourceType.BOOKKEEPING)
public class BookkeepingService {

	@Autowired
	private BookkeepingRepository bookkeepingRepository;
	
	public Bookkeeping save(Bookkeeping bookkeeping) {
		return bookkeepingRepository.save(bookkeeping);
	}
	
	@Transactional(readOnly = true)
	public Bookkeeping findOne(long id) {
		return bookkeepingRepository.findOne(id);
	}
	
	@Transactional(readOnly = true)
	public List<Bookkeeping> findByUserId(long userId) {
		return bookkeepingRepository.findByUserId(userId);
	}
}
