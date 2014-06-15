package net.luversof.bookkeeping.service;

import java.util.List;

import net.luversof.bookkeeping.domain.EntryGroup;
import net.luversof.bookkeeping.repository.EntryGroupRepository;
import net.luversof.jdbc.datasource.DataSource;
import net.luversof.jdbc.datasource.DataSourceType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@DataSource(DataSourceType.BOOKKEEPING)
public class EntryGroupService {
	@Autowired
	private EntryGroupRepository entryGroupRepository;

	public EntryGroup save(EntryGroup entryGroup) {
		return entryGroupRepository.save(entryGroup);
	}

	@Transactional(readOnly = true)
	public EntryGroup findOne(long id) {
		return entryGroupRepository.findOne(id);
	}

	public void delete(long id) {
		entryGroupRepository.delete(id);
	}

	@Transactional(readOnly = true)
	public List<EntryGroup> findByUsername(int id) {
		return entryGroupRepository.findByUserId(id);
	}
}
