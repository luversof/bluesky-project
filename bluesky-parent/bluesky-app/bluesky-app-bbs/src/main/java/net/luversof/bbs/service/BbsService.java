package net.luversof.bbs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.bbs.constant.BbsErrorCode;
import net.luversof.bbs.domain.Bbs;
import net.luversof.bbs.repository.BbsRepository;

@Service
public class BbsService {

	@Autowired
	private BbsRepository bbsRepository;
	
	
	public Bbs save(Bbs bbs) {
		return bbsRepository.save(bbs);
	}
	
	public Bbs findByAlias(String alias) {
		return bbsRepository.findByAlias(alias).orElseThrow(() -> new BlueskyException(BbsErrorCode.NOT_EXIST_BOARD));
	}
}
