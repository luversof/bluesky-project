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

import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.service.CompositeAssetService;

@RestController
@RequestMapping(value = "/api/bookkeeping/asset", produces = MediaType.APPLICATION_JSON_VALUE)
public class AssetController {

	@Autowired
	private CompositeAssetService assetService;
	
	/**
	 * requestBody에 @PathVariable의 id가 맵핑 안되는 부분은 어떻게 처리해야할까?
	 * @param asset
	 * @return
	 */
	@PostMapping
	public Asset create(@RequestBody @Validated(Asset.Create.class) Asset asset) {
		return assetService.create(asset);
	}
	
	/**
	 * 해당 bookkeeping의 자산 리스트 반환
	 * @param bookkeepingId
	 * @return
	 */
	@GetMapping
	public List<Asset> findByBookkeepingId(String bookkeepingId) {
		return assetService.findByBookkeepingId(bookkeepingId);
	}

	@PutMapping
	public Asset update(@RequestBody @Validated(Asset.Update.class) Asset asset) {
		return assetService.update(asset);
	}

	@DeleteMapping
	public void delete(@RequestBody @Validated(Asset.Delete.class) Asset asset) {
		assetService.delete(asset);
	}
	 
}
