package net.luversof.web.bookkeeping.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.service.BookkeepingService;
import net.luversof.security.core.userdetails.BlueskyUser;
import net.luversof.web.constant.AuthorizeRole;

@RestController
@RequestMapping(value = "/bookkeeping")
public class BookkeepingController {

	@Autowired
	private BookkeepingService bookkeepingService;
	
	/**
	 * 로그인한 유저의 bookkeeping 리스트 반환
	 * @param authentication
	 * @return
	 */
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)	
	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<Bookkeeping> getBookkeepingList(Authentication authentication) {
		return bookkeepingService.findByUserId(((BlueskyUser) authentication.getPrincipal()).getId());
	}
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)	
	@RequestMapping(value = "/{id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public Bookkeeping getBookkeeping(@PathVariable long id) {
		return bookkeepingService.findOne(id);
	}
	
	
	@RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public Bookkeeping createBookkeeping(@RequestBody @Validated(Bookkeeping.Create.class) Bookkeeping bookkeeping, Authentication authentication) {
		bookkeeping.setUserId(((BlueskyUser) authentication.getPrincipal()).getId());
		return bookkeepingService.create(bookkeeping);
	}
	
	@RequestMapping(value="/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	@PreAuthorize("hasRole('ROLE_USER') and authentication.principal.id == #bookkeeping.userId")
	public Bookkeeping updateBookkeeping(@RequestBody @Validated(Bookkeeping.Update.class) Bookkeeping bookkeeping) {
		return bookkeepingService.update(bookkeeping);
	}
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)	
	@RequestMapping(value="/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Bookkeeping deleteBookkeeping(@Validated(Bookkeeping.Delete.class) Bookkeeping bookkeeping, Authentication authentication) {
		bookkeeping.setUserId(((BlueskyUser) authentication.getPrincipal()).getId());
		bookkeepingService.delete(bookkeeping);
		return bookkeeping;
	}
}