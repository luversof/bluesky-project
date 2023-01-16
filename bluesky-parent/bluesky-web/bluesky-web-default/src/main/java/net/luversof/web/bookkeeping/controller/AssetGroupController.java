//package net.luversof.web.bookkeeping.controller;
//
//import java.util.List;
//
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.MediaType;
//import org.springframework.validation.annotation.Validated;
//import org.springframework.web.bind.annotation.DeleteMapping;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.PutMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import io.github.luversof.boot.autoconfigure.security.annotation.BlueskyPreAuthorize;
//import net.luversof.bookkeeping.domain.AssetGroup;
//import net.luversof.bookkeeping.service.CompositeAssetGroupService;
//import net.luversof.bookkeeping.service.CompositeBookkeepingService;
//import net.luversof.security.core.userdetails.BlueskyUser;
//
//@RestController
//@BlueskyPreAuthorize
//@RequestMapping(value = "/api/bookkeeping/assetGroup", produces = MediaType.APPLICATION_JSON_VALUE)
//public class AssetGroupController {
//	
//	@Autowired
//	private CompositeAssetGroupService assetGroupService;
//	
//	@Autowired
//	private CompositeBookkeepingService bookkeepingService;
//	
//	@PostMapping
//	public AssetGroup create(@RequestBody @Validated(AssetGroup.Create.class) AssetGroup assetGroup, BlueskyUser blueskyUser) {
//		var bookkeepingList = bookkeepingService.findByUserId(blueskyUser.getId());
//		assetGroup.setBookkeepingId(bookkeepingList.stream().findFirst().get().getBookkeepingId());
//		return assetGroupService.create(assetGroup);
//	}
//	
//	@GetMapping
//	public List<AssetGroup> findByBookkeepingId(BlueskyUser blueskyUser) {
//		var bookkeepingList = bookkeepingService.findByUserId(blueskyUser.getId());
//		return assetGroupService.findByBookkeepingId(bookkeepingList.stream().findFirst().get().getBookkeepingId());
//	}
//	
//	@PutMapping
//	public AssetGroup update(@RequestBody @Validated(AssetGroup.Update.class) AssetGroup assetGroup, BlueskyUser blueskyUser) {
//		var bookkeepingList = bookkeepingService.findByUserId(blueskyUser.getId());
//		assetGroup.setBookkeepingId(bookkeepingList.stream().findFirst().get().getBookkeepingId());
//		return assetGroupService.update(assetGroup);
//	}
//	
//	@DeleteMapping
//	public void delete(@RequestBody @Validated(AssetGroup.Delete.class) AssetGroup assetGroup, BlueskyUser blueskyUser) {
//		var bookkeepingList = bookkeepingService.findByUserId(blueskyUser.getId());
//		assetGroup.setBookkeepingId(bookkeepingList.stream().findFirst().get().getBookkeepingId());
//		assetGroupService.delete(assetGroup);
//	}
//
//}
