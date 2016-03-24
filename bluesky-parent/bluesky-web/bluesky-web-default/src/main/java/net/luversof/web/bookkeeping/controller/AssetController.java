package net.luversof.web.bookkeeping.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.domain.Asset.AssetCreate;
import net.luversof.bookkeeping.domain.Asset.AssetUpdate;
import net.luversof.bookkeeping.service.AssetService;
import net.luversof.web.constant.AuthorizeRole;

@RestController
@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
@RequestMapping(value = "bookkeeping/{bookkeeping.id}/asset")
public class AssetController {

	@Autowired
	private AssetService assetService;
	
	/**
	 * 해당 bookkeeping의 자산 리스트 반환
	 * @param bookkeepingId
	 * @param authentication
	 * @param modelMap
	 * @return
	 */
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@PostAuthorize("(returnObject == null or returnObject.size() == 0) or returnObject.get(0).bookkeeping.userId == authentication.principal.id")
	@RequestMapping(method = RequestMethod.GET)
	public List<Asset> getAssetList(@PathVariable("bookkeeping.id") long bookkeepingId, Authentication authentication, ModelMap modelMap) {
		return assetService.findByBookkeepingId(bookkeepingId);
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.POST)
	public Asset add(@RequestBody @Validated(AssetCreate.class) Asset asset) {
		return assetService.save(asset);
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{id}", method = RequestMethod.PUT)
	public Asset modify(Authentication authentication, @RequestBody @Validated(AssetUpdate.class) Asset asset) {
		return assetService.update(asset);
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public void delete(Authentication authentication, @RequestBody @Validated(AssetUpdate.class) Asset asset) {
		assetService.delete(asset);
	}
	
	 
}
