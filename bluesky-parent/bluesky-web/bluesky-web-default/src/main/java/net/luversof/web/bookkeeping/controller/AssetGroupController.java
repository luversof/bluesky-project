package net.luversof.web.bookkeeping.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.bookkeeping.domain.AssetGroup;
import net.luversof.bookkeeping.service.AssetGroupService;
import net.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;
import net.luversof.security.core.userdetails.BlueskyUser;

@RestController
@BlueskyPreAuthorize
@RequestMapping(value = "/api/bookkeeping/assetGroup", produces = MediaType.APPLICATION_JSON_VALUE)
public class AssetGroupController {
	
	@Autowired
	private AssetGroupService assetGroupService;
	
	@GetMapping
	public List<AssetGroup> getUserAssetGroupList(BlueskyUser blueskyUser) {
		return assetGroupService.getUserAssetGroupList(blueskyUser.getId());
	}

}
