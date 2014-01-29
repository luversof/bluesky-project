package net.luversof.web.bookkeeping.controller;

import lombok.extern.slf4j.Slf4j;
import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.service.AssetGroupService;
import net.luversof.bookkeeping.service.AssetService;
import net.luversof.data.jpa.exception.BlueskyException;
import net.luversof.web.AuthorizeRole;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequestMapping("/user/{userId}/bookkeeping/asset")
public class AssetController {
	
	@Autowired
	private AssetService assetService;
	
	@Autowired
	private AssetGroupService assetGroupService;

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE + " && #userId == authentication.principal.id")
	@RequestMapping(method = RequestMethod.GET, value = "")
	public String list(@PathVariable long userId, @RequestParam(defaultValue = "1") int page, Authentication authentication, ModelMap modelMap) {
		log.debug("modelMap : {}", modelMap);
		modelMap.addAttribute("result", assetService.findByUsername(authentication.getName()));
		modelMap.addAttribute(assetGroupService.findByUsername(authentication.getName()));
		return "/bookkeeping/asset/list";
	}
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE + " && #userId == authentication.principal.id")
	@RequestMapping(method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public void save(@PathVariable long userId, @RequestBody Asset asset, Authentication authentication, ModelMap modelMap) {
		asset.setUsername(authentication.getName());
		log.debug("save asset : {}", asset);
		if (asset.getAssetGroup() != null && asset.getAssetGroup().getId() != 0) {
			asset.setAssetGroup(assetGroupService.findOne(asset.getAssetGroup().getId()));
		}
		modelMap.addAttribute("result", assetService.save(asset));
	}
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE + " && #userId == authentication.principal.id")
	@RequestMapping(value = "/{assetId}", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	public void modify(@PathVariable long userId, @RequestBody Asset asset, Authentication authentication, ModelMap modelMap) {
		Asset targetAsset = assetService.findOne(asset.getId());
		//TODO assetGroupNanem 변경도 적용해야함
		if (!authentication.getName().equals(targetAsset.getUsername())) {
			throw new BlueskyException("username is not owner");
		}
		targetAsset.setName(asset.getName());
		targetAsset.setAmount(asset.getAmount());
		targetAsset.setEnable(asset.isEnable());
		
		modelMap.addAttribute("result", assetService.save(targetAsset));
	}
	
	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE + " && #userId == authentication.principal.id")
	@RequestMapping(value = "/{assetId}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public void delete(@PathVariable long userId, @PathVariable long assetId, Authentication authentication, ModelMap modelMap) {
		Asset targetAsset = assetService.findOne(assetId);
		if (!authentication.getName().equals(targetAsset.getUsername())) {
			throw new BlueskyException("username is not owner");
		}
		log.debug("id : {}", assetId);
		assetService.delete(assetId);
	}
}
