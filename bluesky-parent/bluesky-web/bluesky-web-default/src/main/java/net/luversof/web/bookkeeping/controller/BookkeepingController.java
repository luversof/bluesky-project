//package net.luversof.web.bookkeeping.controller;
//
//import java.util.List;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.MediaType;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.DeleteMapping;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.PutMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import io.github.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;
//import net.luversof.bookkeeping.domain.Bookkeeping;
//import net.luversof.bookkeeping.service.CompositeBookkeepingService;
//import net.luversof.security.core.userdetails.BlueskyUser;
//
//@RestController
//@BlueskyPreAuthorize
//@RequestMapping(value = "/api/bookkeeping", produces = MediaType.APPLICATION_JSON_VALUE)
//public class BookkeepingController {
//
//	@Autowired
//	private CompositeBookkeepingService bookkeepingService;
//	
//	@PostMapping
//	public Bookkeeping create(BlueskyUser blueskyUser, @RequestBody @Validated(Bookkeeping.Create.class) Bookkeeping bookkeeping) {
//		bookkeeping.setUserId(blueskyUser.getId());
//		return bookkeepingService.create(bookkeeping);
//	}
//	
//	/**
//	 * 로그인한 유저의 bookkeeping 리스트 반환
//	 * @param authentication
//	 * @return
//	 */
//	@GetMapping
//	public List<Bookkeeping> findByUserId(BlueskyUser blueskyUser) {
//		return bookkeepingService.findByUserId(blueskyUser.getId());
//	}
//	
//	@PutMapping
//	public Bookkeeping update(BlueskyUser blueskyUser, @RequestBody @Validated(Bookkeeping.Update.class) Bookkeeping bookkeeping) {
//		bookkeeping.setUserId(blueskyUser.getId());
//		return bookkeepingService.update(bookkeeping);
//	}
//	
//	@DeleteMapping
//	public void delete(BlueskyUser blueskyUser) {
//		Bookkeeping bookkeeping = new Bookkeeping();
//		bookkeeping.setUserId(blueskyUser.getId());
//		bookkeepingService.delete(bookkeeping);
//	}
//}