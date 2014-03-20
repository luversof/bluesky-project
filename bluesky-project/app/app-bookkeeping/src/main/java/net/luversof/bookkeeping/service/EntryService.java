package net.luversof.bookkeeping.service;

import java.util.List;

import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.repository.EntryRepository;
import net.luversof.data.jpa.datasource.DataSource;
import net.luversof.data.jpa.datasource.DataSourceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@DataSource(DataSourceType.BOOKKEEPING)
public class EntryService {
	@Autowired
	private EntryRepository entryRepository;

	public Entry save(Entry entry) {
		return entryRepository.save(entry);
	}

	@Transactional(readOnly = true)
	public Entry findOne(long id) {
		return entryRepository.findOne(id);
	}

	public void delete(long id) {
		entryRepository.delete(id);
	}
	
	@Transactional(readOnly = true)
	public List<Entry> findByAssetUsername(int id) {
		return entryRepository.findByAssetUserId(id);
	}
}
