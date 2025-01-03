package net.luversof.api.bookkeeping.controller.base;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Getter;
import lombok.Setter;
import net.luversof.api.bookkeeping.domain.Entry;
import net.luversof.api.bookkeeping.service.base.EntryBaseService;

@RestController
@RequestMapping(value = "/api/entry/base", produces = MediaType.APPLICATION_JSON_VALUE)
public class EntryBaseController implements BaseController<Entry, UUID> {

	@Setter(onMethod_ = @Autowired)
	@Getter
	private EntryBaseService service;
	
}
