package net.luversof.api.bookkeeping.service.base;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.Setter;
import net.luversof.api.bookkeeping.domain.Bookkeeping;
import net.luversof.api.bookkeeping.repository.mariadb.BookkeepingRepository;

@Service
public class BookkeepingBaseService implements BaseService<Bookkeeping, UUID> {
	
	@Getter
	@Setter(onMethod_ = @Autowired)
	private BookkeepingRepository repository;

	public List<Bookkeeping> findByUserId(UUID userId) {
		return repository.findByUserId(userId);
	}

}
