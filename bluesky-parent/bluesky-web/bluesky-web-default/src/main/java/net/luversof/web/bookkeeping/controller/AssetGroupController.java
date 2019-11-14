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

import net.luversof.bookkeeping.domain.AssetGroup;
import net.luversof.bookkeeping.domain.Bookkeeping;
import net.luversof.bookkeeping.service.AssetGroupService;
import net.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;
import net.luversof.security.core.userdetails.BlueskyUser;

@RestController
@BlueskyPreAuthorize
@RequestMapping(value = "/api/bookkeeping/assetGroup", produces = MediaType.APPLICATION_JSON_VALUE)
public class AssetGroupController {
	
	@Autowired
	private AssetGroupService assetGroupService;
	
	@PostMapping
	public AssetGroup createUserAssetGroup(@RequestBody @Validated(AssetGroup.Create.class) AssetGroup assetGroup, BlueskyUser blueskyUser) {
		assetGroup.setBookkeeping(new Bookkeeping());
		assetGroup.getBookkeeping().setUserId(blueskyUser.getId());
		return assetGroupService.createUserAssetGroup(assetGroup);
	}
	
	@GetMapping
	public List<AssetGroup> getUserAssetGroupList(BlueskyUser blueskyUser) {
		return assetGroupService.getUserAssetGroupList(blueskyUser.getId());
	}
	
	@PutMapping
	public AssetGroup updateUserAssetGroup(@RequestBody @Validated(AssetGroup.Update.class) AssetGroup assetGroup, BlueskyUser blueskyUser) {
		assetGroup.setBookkeeping(new Bookkeeping());
		assetGroup.getBookkeeping().setUserId(blueskyUser.getId());
		return assetGroupService.updateUserAssetGroup(assetGroup);
	}
	
	@DeleteMapping
	public void deleteUserAssetGroup(@RequestBody @Validated(AssetGroup.Delete.class) AssetGroup assetGroup, BlueskyUser blueskyUser) {
		assetGroup.setBookkeeping(new Bookkeeping());
		assetGroup.getBookkeeping().setUserId(blueskyUser.getId());
		assetGroupService.deleteUserAssetGroup(assetGroup);
	}

}
