package net.luversof.api.bookkeeping.controller;

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
import net.luversof.bookkeeping.service.CompositeAssetGroupService;

@RestController
@RequestMapping(value = "/api/bookkeeping/assetGroup", produces = MediaType.APPLICATION_JSON_VALUE)
public class AssetGroupController {
	
	@Autowired
	private CompositeAssetGroupService assetGroupService;
	
	@PostMapping
	public AssetGroup create(@RequestBody @Validated(AssetGroup.Create.class) AssetGroup assetGroup) {
		return assetGroupService.create(assetGroup);
	}
	
	@GetMapping
	public List<AssetGroup> findByBookkeepingId(@RequestBody String bookkeepingId) {
		return assetGroupService.findByBookkeepingId(bookkeepingId);
	}
	
	@PutMapping
	public AssetGroup update(@RequestBody @Validated(AssetGroup.Update.class) AssetGroup assetGroup) {
		return assetGroupService.update(assetGroup);
	}
	
	@DeleteMapping
	public void delete(@RequestBody @Validated(AssetGroup.Delete.class) AssetGroup assetGroup) {
		assetGroupService.delete(assetGroup);
	}

}
