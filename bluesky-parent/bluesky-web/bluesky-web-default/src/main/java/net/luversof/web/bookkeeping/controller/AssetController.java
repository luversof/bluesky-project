package net.luversof.web.bookkeeping.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.domain.Asset.Add;
import net.luversof.bookkeeping.domain.Asset.Modify;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.service.AssetService;
import net.luversof.bookkeeping.service.BookkeepingService;
import net.luversof.security.core.userdetails.BlueskyUser;
import net.luversof.web.constant.AuthorizeRole;

@RestController
@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
@RequestMapping(value = "bookkeeping/{bookkeeping.id}/asset")
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
	
	@RequestMapping(method = RequestMethod.GET)
	public List<Asset> getAssetList(Authentication authentication, ModelMap modelMap) {
		Bookkeeping bookkeeping = getBookkeeping(authentication);
		return assetService.findByBookkeepingId(bookkeeping.getId());
	}

	@RequestMapping(method = RequestMethod.POST)
	public Asset add(Authentication authentication, @RequestBody @Validated(Add.class) Asset asset) {
		asset.setBookkeeping(getBookkeeping(authentication));
		return assetService.save(asset);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.PUT)
	public Asset modify(Authentication authentication, @RequestBody @Validated(Modify.class) Asset asset) {
		//TODO 본인 entryGroup 확인 절차가 있어야 함
		asset.setBookkeeping(getBookkeeping(authentication));
		return assetService.save(asset);
	}

	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public void delete(Authentication authentication, @RequestBody @Validated(Modify.class) Asset asset) {
		//TODO 본인 entryGroup 확인 절차가 있어야 함
		asset.setBookkeeping(getBookkeeping(authentication));
		assetService.delete(asset);
	}
}
