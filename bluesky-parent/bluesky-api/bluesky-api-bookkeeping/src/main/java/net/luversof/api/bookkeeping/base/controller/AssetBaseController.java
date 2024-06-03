package net.luversof.api.bookkeeping.base.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import net.luversof.api.bookkeeping.base.domain.Asset;
import net.luversof.api.bookkeeping.base.service.AssetService;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/asset/base", produces = MediaType.APPLICATION_JSON_VALUE)
public class AssetBaseController {

	private final AssetService assetBaseService;
	
	@PutMapping
	public Asset update(@RequestBody @Validated(Asset.Update.class) Asset asset) {
		return assetBaseService.update(asset);
	}
	
	@GetMapping("/{id}")
	public Optional<Asset> findById(@PathVariable UUID id) {
		return assetBaseService.findById(id);
	}
	
	@GetMapping("/search/findByBookkeepingId/{bookkeepingId}")
	public List<Asset> findByBookkeepingId(@PathVariable UUID ledgerId) {
		return assetBaseService.findByLedgerId(ledgerId);
	}

}
