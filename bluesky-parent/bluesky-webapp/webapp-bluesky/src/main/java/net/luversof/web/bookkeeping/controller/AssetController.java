package net.luversof.web.bookkeeping.controller;

import static net.luversof.core.Constants.JSON_MODEL_KEY;
import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.domain.Asset.Add;
import net.luversof.bookkeeping.domain.Asset.Modify;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.service.AssetService;
import net.luversof.bookkeeping.service.BookkeepingService;
import net.luversof.security.core.userdetails.BlueskyUser;
import net.luversof.web.AuthorizeRole;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/bookkeeping/asset")
public class AssetController {
	
	@Autowired
	private BookkeepingService bookkeepingService;
	
	@Autowired
	private AssetService assetService;
	
	/**
	 * 가계부가 1개만 존재한다는 전제조건으로 설정
	 * @author bluesky
	 *
	 */
	private Bookkeeping getBookkeeping(Authentication authentication) {
		return bookkeepingService.findByUserId(((BlueskyUser) authentication.getPrincipal()).getId()).get(0);
	}
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.GET)
	public void get(Authentication authentication, ModelMap modelMap) {
		Bookkeeping bookkeeping = getBookkeeping(authentication);
		modelMap.addAttribute(JSON_MODEL_KEY , assetService.findByBookkeepingId(bookkeeping.getId()));
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Asset add(Authentication authentication, @RequestBody @Validated(Add.class) Asset asset) {
		asset.setBookkeeping(getBookkeeping(authentication));
		return assetService.save(asset);
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Asset modify(Authentication authentication, @RequestBody @Validated(Modify.class) Asset asset) {
		//TODO 본인 entryGroup 확인 절차가 있어야 함
		asset.setBookkeeping(getBookkeeping(authentication));
		return assetService.save(asset);
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public void delete(Authentication authentication, @RequestBody @Validated(Modify.class) Asset asset) {
		//TODO 본인 entryGroup 확인 절차가 있어야 함
		asset.setBookkeeping(getBookkeeping(authentication));
		assetService.delete(asset);
	}
}
