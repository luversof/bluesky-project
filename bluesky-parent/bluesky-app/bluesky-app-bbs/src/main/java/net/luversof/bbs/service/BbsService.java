package net.luversof.bbs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.bbs.domain.Bbs;
import net.luversof.bbs.repository.BbsRepository;
import net.luversof.core.exception.BlueskyException;

@Service
public class BbsService {

	@Autowired
	private BbsRepository bbsRepository;
	
	
	public Bbs save(Bbs bbs) {
		return bbsRepository.save(bbs);
	}
	
	public Bbs findOne(long bbsId) {
		return bbsRepository.getOne(bbsId);
	}
	
	public Bbs findByAlias(String alias) {
		return bbsRepository.findByAlias(alias).orElseThrow(() -> new BlueskyException("bbs.NOT_EXIST_BOARD"));
	}
}
