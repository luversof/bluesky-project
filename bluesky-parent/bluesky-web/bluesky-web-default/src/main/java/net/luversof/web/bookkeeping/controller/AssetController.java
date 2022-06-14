package net.luversof.web.bookkeeping.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;
import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.service.BasicAssetService;
import net.luversof.security.core.userdetails.BlueskyUser;

@RestController
@BlueskyPreAuthorize
@RequestMapping(value = "/api/bookkeeping/asset", produces = MediaType.APPLICATION_JSON_VALUE)
public class AssetController {

	@Autowired
	private BasicAssetService assetService;

	/**
	 * requestBody에 @PathVariable의 id가 맵핑 안되는 부분은 어떻게 처리해야할까?
	 * @param asset
	 * @param bookkeepingId
	 * @param authentication
	 * @return
	 */
	@PostMapping
	public Asset createUserAsset(BlueskyUser blueskyUser, @RequestBody @Validated(Asset.Create.class) Asset asset) {
		asset.setBookkeeping(new Bookkeeping());
		asset.getBookkeeping().setUserId(blueskyUser.getId());
		return assetService.createUserAsset(asset);
	}
	
	/**
	 * 해당 bookkeeping의 자산 리스트 반환
	 * @param bookkeepingId
	 * @param authentication
	 * @param modelMap
	 * @return
	 */
	@GetMapping
	public List<Asset> getUserAssetList(BlueskyUser blueskyUser) {
		return assetService.getUserAssetList(blueskyUser.getId());
	}

	@PutMapping
	public Asset updateUserAsset(BlueskyUser blueskyUser, @RequestBody @Validated(Asset.Update.class) Asset asset) {
		asset.setBookkeeping(new Bookkeeping());
		asset.getBookkeeping().setUserId(blueskyUser.getId());
		return assetService.updateUserAsset(asset);
	}

	@DeleteMapping
	public void deleteUserAsset(BlueskyUser blueskyUser, @RequestBody @Validated(Asset.Delete.class) Asset asset) {
		asset.setBookkeeping(new Bookkeeping());
		asset.getBookkeeping().setUserId(blueskyUser.getId());
		assetService.deleteUserAsset(asset);
	}
	 
}
