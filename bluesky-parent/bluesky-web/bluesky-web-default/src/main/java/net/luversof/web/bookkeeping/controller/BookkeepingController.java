package net.luversof.web.bookkeeping.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.domain.Bookkeeping.Modify;
import net.luversof.bookkeeping.service.BookkeepingService;
import net.luversof.security.core.userdetails.BlueskyUser;
import net.luversof.web.constant.AuthorizeRole;

@RestController

@RequestMapping(value = "bookkeeping")
public class BookkeepingController {

	@Autowired
	private BookkeepingService bookkeepingService;
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)	
	@RequestMapping(value= "", method = RequestMethod.GET)
	public List<Bookkeeping> getMyBookkeeping(Authentication authentication) {
		return bookkeepingService.findByUserId(((BlueskyUser) authentication.getPrincipal()).getId());
	}
	
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public Bookkeeping getBookkeeping(@PathVariable long id) {
		return bookkeepingService.findOne(id);
	}
	
	@RequestMapping(value="/{id}", method = RequestMethod.PUT)
	public Bookkeeping modifyBookkeeping(Authentication authentication, @RequestBody @Validated(Modify.class) Bookkeeping bookkeeping) {
		//TODO 본인 bookkeeping 확인 절차가 있어야 함
		bookkeeping.setUserId(((BlueskyUser) authentication.getPrincipal()).getId());
		return bookkeepingService.save(bookkeeping);
	}
	
	@RequestMapping(value="/{id}", method = RequestMethod.DELETE)
	public void removeBookkeeping(Authentication authentication, @RequestBody @Validated(Modify.class) Bookkeeping bookkeeping) {
		//TODO 본인 bookkeeping 확인 절차가 있어야 함
		bookkeeping.setUserId(((BlueskyUser) authentication.getPrincipal()).getId());
		bookkeepingService.delete(bookkeeping);
	}
}
