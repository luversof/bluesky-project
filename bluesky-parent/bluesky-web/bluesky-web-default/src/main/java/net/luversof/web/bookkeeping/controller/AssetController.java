package net.luversof.web.bookkeeping.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.domain.Asset.AssetCreate;
import net.luversof.bookkeeping.domain.Asset.AssetDelete;
import net.luversof.bookkeeping.domain.Asset.AssetUpdate;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.service.AssetService;
import net.luversof.security.core.userdetails.BlueskyUser;
import net.luversof.web.constant.AuthorizeRole;

@RestController
@RequestMapping(value = "/bookkeeping/{bookkeepingId}/asset")
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
	public List<Asset> getAssetList(@PathVariable long bookkeepingId, Authentication authentication) {
		return assetService.findByBookkeepingId(bookkeepingId);
	}

	/**
	 * requestBody에 @PathVariable의 id가 맵핑 안되는 부분은 어떻게 처리해야할까?
	 * @param asset
	 * @param bookkeepingId
	 * @param authentication
	 * @return
	 */
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(method = RequestMethod.POST)
	public Asset createAsset(@RequestBody @Validated(AssetCreate.class) Asset asset, @PathVariable long bookkeepingId, Authentication authentication) {
		Bookkeeping bookkeeping = new Bookkeeping();
		bookkeeping.setId(bookkeepingId);
		BlueskyUser blueskyUser = (BlueskyUser) authentication.getPrincipal();
		bookkeeping.setUserId(blueskyUser.getId());
		asset.setBookkeeping(bookkeeping);
		return assetService.create(asset);
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{id}", method = RequestMethod.PUT)
	public Asset updateAsset(@RequestBody @Validated(AssetUpdate.class) Asset asset, Authentication authentication) {
		asset.getBookkeeping().setUserId(((BlueskyUser) authentication.getPrincipal()).getId());
		return assetService.update(asset);
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public void deleteAsset(@Validated(AssetDelete.class) Asset asset, Authentication authentication) {
		asset.getBookkeeping().setUserId(((BlueskyUser) authentication.getPrincipal()).getId());
		assetService.delete(asset);
	}
	
	 
}
