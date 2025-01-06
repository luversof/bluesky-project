package net.luversof.api.bookkeeping.controller.base;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Getter;
import lombok.Setter;
import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.service.base.AssetBaseService;


@RestController
@RequestMapping(value = "/api/assets/base", produces = MediaType.APPLICATION_JSON_VALUE)
public class AssetBaseController implements BaseController<Asset, UUID> {

	@Setter(onMethod_ = @Autowired)
	@Getter
	private AssetBaseService service;
	
	@Setter(onMethod_ = @Autowired)
	@Getter
	private ObjectMapper objectMapper;

}
