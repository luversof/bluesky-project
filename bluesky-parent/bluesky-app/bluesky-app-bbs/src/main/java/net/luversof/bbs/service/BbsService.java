package net.luversof.bbs.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.luversof.bbs.domain.Bbs;
import net.luversof.bbs.repository.BbsRepository;

@Service
public class BbsService {

	@Autowired
	private BbsRepository bbsRepository;
	
	
	public Bbs save(Bbs bbs) {
		return bbsRepository.save(bbs);
	}
	
	public Bbs findOne(long bbsId) {
		return bbsRepository.findOne(bbsId);
	}
	
	public Bbs findByAliasName(String aliasName) {
		return bbsRepository.findByAliasName(aliasName);
	}
}
