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
import net.luversof.bookkeeping.domain.Bookkeeping.Create;
import net.luversof.bookkeeping.domain.Bookkeeping.Delete;
import net.luversof.bookkeeping.domain.Bookkeeping.Update;
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
	
	
	@RequestMapping(value = "", method = RequestMethod.POST)
	public Bookkeeping post(@RequestBody @Validated(Create.class) Bookkeeping bookkeeping, Authentication authentication) {
		BlueskyUser blueskyUser = (BlueskyUser) authentication.getPrincipal();
		bookkeeping.setUserId(blueskyUser.getId());
		return bookkeepingService.save(bookkeeping);
	}
	
	@RequestMapping(value="/{id}", method = RequestMethod.PUT)
	@PreAuthorize("hasRole('ROLE_USER') and authentication.principal.id == #bookkeeping.userId")
	public Bookkeeping modifyBookkeeping(Authentication authentication, @RequestBody @Validated(Update.class) Bookkeeping bookkeeping) {
		return bookkeepingService.update(bookkeeping);
	}
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)	
	@RequestMapping(value="/{id}", method = RequestMethod.DELETE)
	public Bookkeeping removeBookkeeping(Authentication authentication, @Validated(Delete.class) Bookkeeping bookkeeping) {
		//TODO 본인 bookkeeping 확인 절차가 있어야 함
		bookkeeping.setUserId(((BlueskyUser) authentication.getPrincipal()).getId());
		bookkeepingService.delete(bookkeeping);
		return bookkeeping;
	}
}
