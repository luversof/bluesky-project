package net.luversof.bookkeeping.service;

import java.util.List;
import java.util.Optional;

import io.github.luversof.boot.autoconfigure.validation.annotation.BlueskyValidated;
import net.luversof.bookkeeping.domain.EntryGroup;

public interface EntryGroupService {

	List<EntryGroup> createInitialData(String bookkeepingId);
	
	EntryGroup create(@BlueskyValidated(EntryGroup.Create.class) EntryGroup entryGroup);
	
	Optional<EntryGroup> findByEntryGroupId(String entryGroupId);
	
	List<EntryGroup> findByBookkeepingId(String bookkeepingId);
	
	EntryGroup update(@BlueskyValidated(EntryGroup.Update.class)EntryGroup entryGroup);
	
	void delete(@BlueskyValidated(EntryGroup.Delete.class) EntryGroup entryGroup);
	
	void deleteAllBybookkeepingId(String bookkeepingId);
}
