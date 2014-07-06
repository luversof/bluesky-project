package net.luversof.bookkeeping.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.EntryGroupInitialData;
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
	
	/**
	 * 초기 데이터 insert
	 * @param bookkeeping
	 * @return
	 */
	public List<EntryGroup> initialDataSave(Bookkeeping bookkeeping) {
		Set<EntryGroup> entryGroupSet = new HashSet<>();
		for (EntryGroupInitialData entryGroupInitialData : EntryGroupInitialData.values()) {
			for (String defaultEntryGroupName : entryGroupInitialData.getDefaltEntryGroupNames()){
				EntryGroup entryGroup = new EntryGroup();
				entryGroup.setEntryType(entryGroupInitialData.getEntryType());
				entryGroup.setName(defaultEntryGroupName);
				entryGroup.setBookkeeping(bookkeeping);
				entryGroupSet.add(entryGroup);
			}
		}
		return entryGroupRepository.save(entryGroupSet);
	}
	public EntryGroup save(EntryGroup entryGroup) {
		return entryGroupRepository.save(entryGroup);
	}

	@Transactional(readOnly = true)
	public EntryGroup findOne(long id) {
		return entryGroupRepository.findOne(id);
	}
	
	@Transactional(readOnly = true)
	public List<EntryGroup> findByBookkeepingId(long bookkeeping_id) {
		return entryGroupRepository.findByBookkeepingId(bookkeeping_id);
	}

	public void delete(EntryGroup entryGroup) {
		entryGroupRepository.delete(entryGroup);
	}
	
	public void deleteBybookkeepingId(long bookkeeping_id) {
		List<EntryGroup> entryGroupList = findByBookkeepingId(bookkeeping_id);
		entryGroupRepository.delete(entryGroupList);
	}
}
