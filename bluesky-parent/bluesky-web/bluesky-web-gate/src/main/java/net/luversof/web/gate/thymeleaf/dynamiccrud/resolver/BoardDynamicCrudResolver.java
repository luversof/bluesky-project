package net.luversof.web.gate.thymeleaf.dynamiccrud.resolver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import net.luversof.web.gate.feign.board.client.BoardClient;
import net.luversof.web.gate.feign.board.domain.Board;

public class BoardDynamicCrudResolver implements DynamicCrudResolver<Board>{
	
	@Autowired
	private BoardClient boardClient;

	@Override
	public Page<Board> selectAllList(Pageable pageable) {
		// TODO Auto-generated method stub
		return boardClient.findAll(pageable);
	}

	@Override
	public Board select(Board t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Board create(Board t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Board update(Board t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Board delete(Board t) {
		// TODO Auto-generated method stub
		return null;
	}

}
