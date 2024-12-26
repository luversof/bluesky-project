package net.luversof.api.bookkeeping.controller;

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
import net.luversof.api.bookkeeping.domain.AssetType;
import net.luversof.api.bookkeeping.service.AssetTypeService;

@RequiredArgsConstructor
@RestController
@RequestMapping(value = "/api/accountType/base", produces = MediaType.APPLICATION_JSON_VALUE)
public class AssetTypeController {

	private final AssetTypeService assetTypeService;
	
	@PutMapping
	public AssetType update(@RequestBody @Validated(AssetType.Update.class) AssetType accountType) {
		return assetTypeService.update(accountType);
	}
	
	@GetMapping("/{id}")
	public Optional<AssetType> findById(@PathVariable UUID id) {
		return assetTypeService.findById(id);
	}
	
	@GetMapping("/search/findByBookkeepingId/{bookkeepingId}")
	public List<AssetType> findByBookkeepingId(@PathVariable UUID bookkeepingId) {
		return assetTypeService.findByBookkeepingId(bookkeepingId);
	}

}