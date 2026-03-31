package net.luversof.api.bookkeeping.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import net.luversof.api.bookkeeping.domain.Asset;
import net.luversof.api.bookkeeping.service.AssetService;

@RestController
@RequestMapping(value = "/api/asset", produces = MediaType.APPLICATION_JSON_VALUE)
public class AssetController {

    @Autowired private AssetService assetService;

    public void setAssetService(AssetService assetService) {
        this.assetService = assetService;
    }

    @PostMapping
    public Asset createAsset(@RequestBody Asset asset) {
        return assetService.createAsset(asset);
    }

    @GetMapping("/search/findByBookkeepingId/{bookkeepingId}")
    public List<Asset> findByBookkeepingId(@PathVariable UUID bookkeepingId) {
        return assetService.findByBookkeepingId(bookkeepingId);
    }

    @PutMapping
    public Asset updateAsset(Asset asset) {
        return assetService.updateAsset(asset);
    }

    @DeleteMapping
    public void delete(@RequestBody Asset asset) {
        assetService.deleteAsset(asset);
    }
}
