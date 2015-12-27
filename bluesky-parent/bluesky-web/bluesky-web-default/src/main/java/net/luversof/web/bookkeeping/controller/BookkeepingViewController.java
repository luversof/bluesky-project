package net.luversof.web.bookkeeping.controller;

import java.text.MessageFormat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.service.BookkeepingService;

@Controller
@RequestMapping("bookkeeping")
public class BookkeepingViewController {
	
	@Autowired
	private BookkeepingService bookkeepingService;

	@RequestMapping("/$!")
	public String home(Bookkeeping bookkeeping) {
		return redirectEntryList(bookkeeping.getId());
	}
	
	
	@RequestMapping(value = "/{id}", method=RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public String redirectEntryList(@PathVariable long id) {
		return MessageFormat.format("redirect:/bookkeeping/{0}/entry", id);
	}
	
	
	@RequestMapping(value = "/create", method=RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
	public void createForm() {}
	
	
//	@RequestMapping(value = "/{bookkeepingId}/entry", method = RequestMethod.GET, produces = MediaType.TEXT_HTML_VALUE)
//	public String list(@PathVariable long bookkeepingId, @RequestParam(defaultValue = "1") int page, ModelMap modelMap) {
//		// 기본은 달 기준으로 요청을 처리해야할 듯한데..
//		
//	}
}
