package net.luversof.api.bookkeeping.base.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.base.domain.Bookkeeping;
import net.luversof.api.bookkeeping.base.repository.BookkeepingRepository;

@RequiredArgsConstructor
@Service
public class BookkeepingBaseService extends AbstractBaseService<Bookkeeping, UUID> {
	
	@Getter
	private final BookkeepingRepository repository;


}