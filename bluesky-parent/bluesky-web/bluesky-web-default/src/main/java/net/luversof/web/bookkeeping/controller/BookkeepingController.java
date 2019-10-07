package net.luversof.web.bookkeeping.controller;

import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.service.BookkeepingService;
import net.luversof.security.util.SecurityUtil;
import net.luversof.web.constant.AuthorizeRole;

@RestController
@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
@RequestMapping(value = "/bookkeeping", produces = MediaType.APPLICATION_JSON_VALUE)
public class BookkeepingController {

	@Autowired
	private BookkeepingService bookkeepingService;
	
	/**
	 * 로그인한 유저의 bookkeeping 리스트 반환
	 * @param authentication
	 * @return
	 */
	@PreAuthorize("hasRole('ROLE_USER')")
	@GetMapping
	public Optional<Bookkeeping> getBookkeepingList() {
		Bookkeeping bookkeeping = new Bookkeeping();
		bookkeeping.setUserId(SecurityUtil.getUser().get().getId());
		return bookkeepingService.getUserBookkeeping(bookkeeping);
	}

	@GetMapping(value = "/{id}")
	public Optional<Bookkeeping> getBookkeeping(@PathVariable UUID id) {
		Bookkeeping bookkeeping = new Bookkeeping();
		bookkeeping.setId(id);
		return bookkeepingService.getUserBookkeeping(bookkeeping);
	}
	
	@PostMapping
	public Bookkeeping createBookkeeping(@RequestBody @Validated(Bookkeeping.Create.class) Bookkeeping bookkeeping) {
		return bookkeepingService.create(bookkeeping);
	}
	
	@PreAuthorize("hasRole('ROLE_USER') and authentication.principal.id == #bookkeeping.userId")
	@PutMapping(value="/{id}")
	public Bookkeeping updateBookkeeping(@RequestBody @Validated(Bookkeeping.Update.class) Bookkeeping bookkeeping) {
		return bookkeepingService.update(bookkeeping);
	}
	
	@DeleteMapping(value="/{id}")
	public Bookkeeping deleteBookkeeping(@Validated(Bookkeeping.Delete.class) Bookkeeping bookkeeping, Authentication authentication) {
		bookkeeping.setUserId(SecurityUtil.getUser().get().getId());
		bookkeepingService.delete(bookkeeping);
		return bookkeeping;
	}
}