package net.luversof.web.bookkeeping.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
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
import net.luversof.bookkeeping.service.AssetService;
import net.luversof.security.core.userdetails.BlueskyUser;
import net.luversof.web.constant.AuthorizeRole;

@RestController
@RequestMapping(value = "/bookkeeping/{bookkeeping.id}/asset")
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
	@RequestMapping(method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<Asset> getAssetList(@PathVariable("bookkeeping.id") long bookkeepingId, Authentication authentication) {
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
	@RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public Asset createAsset(@RequestBody @Validated(Asset.Create.class) Asset asset, Authentication authentication) {
		asset.getBookkeeping().setUserId(((BlueskyUser) authentication.getPrincipal()).getId());
		return assetService.create(asset);
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{id}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	public Asset updateAsset(@RequestBody @Validated(Asset.Update.class) Asset asset, Authentication authentication) {
		asset.getBookkeeping().setUserId(((BlueskyUser) authentication.getPrincipal()).getId());
		return assetService.update(asset);
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public void deleteAsset(@Validated(Asset.Delete.class) Asset asset, Authentication authentication) {
		asset.getBookkeeping().setUserId(((BlueskyUser) authentication.getPrincipal()).getId());
		assetService.delete(asset);
	}
	
	 
}
