package net.luversof.web.bookkeeping.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PostAuthorize;
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

import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.service.AssetService;
import net.luversof.security.core.userdetails.BlueskyUser;
import net.luversof.web.constant.AuthorizeRole;

//@RestController
@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE)
@RequestMapping(value = "/bookkeeping/{bookkeeping.id}/asset", produces = MediaType.APPLICATION_JSON_VALUE)
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
	@PostAuthorize("(returnObject == null or returnObject.size() == 0) or returnObject.get(0).bookkeeping.userId == authentication.principal.id")
	@GetMapping
	public List<Asset> getAssetList(@PathVariable("bookkeeping.id") UUID bookkeepingId) {
		return assetService.findByBookkeepingId(bookkeepingId);
	}

	/**
	 * requestBody에 @PathVariable의 id가 맵핑 안되는 부분은 어떻게 처리해야할까?
	 * @param asset
	 * @param bookkeepingId
	 * @param authentication
	 * @return
	 */
	@PostMapping
	public Asset createAsset(@RequestBody @Validated(Asset.Create.class) Asset asset, Authentication authentication) {
		asset.getBookkeeping().setUserId(((BlueskyUser) authentication.getPrincipal()).getId());
		return assetService.create(asset);
	}

	@PutMapping(value = "/{id}")
	public Asset updateAsset(@RequestBody @Validated(Asset.Update.class) Asset asset, Authentication authentication) {
		asset.getBookkeeping().setUserId(((BlueskyUser) authentication.getPrincipal()).getId());
		return assetService.update(asset);
	}

	@DeleteMapping(value = "/{id}")
	public void deleteAsset(@Validated(Asset.Delete.class) Asset asset, Authentication authentication) {
		asset.getBookkeeping().setUserId(((BlueskyUser) authentication.getPrincipal()).getId());
		assetService.delete(asset);
	}
	 
}
