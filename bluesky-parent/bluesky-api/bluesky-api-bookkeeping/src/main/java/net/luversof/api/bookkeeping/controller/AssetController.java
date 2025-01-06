package net.luversof.api.bookkeeping.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Setter;
import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.service.AssetService;

@RestController
@RequestMapping(value = "/api/assets", produces = MediaType.APPLICATION_JSON_VALUE)
public class AssetController {

	@Setter(onMethod_ = @Autowired)
	private AssetService assetService;

	@PostMapping
	public Asset createAsset(Asset asset) {
		return assetService.createAsset(asset);
	}
	
	@PutMapping
	public Asset updateAsset(Asset asset) {
		return assetService.updateAsset(asset);
	}
	
	@GetMapping("/search/findByBookkeepingId")
	public List<Asset> findByBookkeepingId(UUID bookkeepingId) {
		return assetService.findByBookkeepingId(bookkeepingId);
	}
	
	
}
