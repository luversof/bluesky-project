package net.luversof.web.bookkeeping.controller;

import lombok.extern.slf4j.Slf4j;
import net.luversof.bookkeeping.domain.Asset;
import net.luversof.bookkeeping.domain.Entry;
import net.luversof.bookkeeping.domain.EntryGroup;
import net.luversof.bookkeeping.service.AssetService;
import net.luversof.bookkeeping.service.EntryGroupService;
import net.luversof.bookkeeping.service.EntryService;
import net.luversof.web.AuthorizeRole;

import org.springframework.beans.factory.annotation.Autowired;
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
@RequestMapping("/user/{userId}/bookkeeping/entry")
public class EntryController {
	
	@Autowired
	private EntryService entryService;
	
	@Autowired
	private AssetService assetService;
	
	@Autowired
	private EntryGroupService entryGroupService;
	
//	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE + " && #userId == authentication.principal.id")
//	@RequestMapping(method = RequestMethod.GET, value = "")
//	public String list(@PathVariable long userId, @RequestParam(defaultValue = "1") int page, Authentication authentication, ModelMap modelMap) {
//		log.debug("modelMap : {}", modelMap);
//		modelMap.addAttribute("result", entryService.findByAssetUsername(authentication.getName()));
//		modelMap.addAttribute(assetService.findByUsername(authentication.getName()));
//		modelMap.addAttribute(entryGroupService.findByUsername(authentication.getName()));
//		return "/bookkeeping/entry/list";
//	}
//	
//	
//	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE + " && #userId == authentication.principal.id")
//	@RequestMapping(method = RequestMethod.POST)
//	public void save(@PathVariable long userId, @RequestBody Entry entry, Authentication authentication, ModelMap modelMap) {
//		log.debug("save entry : {}", entry);
//		
//		if (entry.getAsset() == null || entry.getAsset().getId() <= 0) {
//			throw new BlueskyException("asset is empty");
//		}
//		if (entry.getEntryGroup() == null || entry.getEntryGroup().getId() <= 0) {
//			throw new BlueskyException("assetGroup is empty");
//		}
//		
//		Asset asset = assetService.findOne(entry.getAsset().getId());
//		if (!asset.getUsername().equals(authentication.getName())) {
//			throw new BlueskyException("not owner's asset");
//		}
//		entry.setAsset(asset);
//		
//		EntryGroup entryGroup = entryGroupService.findOne(entry.getEntryGroup().getId());
//		if (!entryGroup.getUsername().equals(authentication.getName())) {
//			throw new BlueskyException("not owner's entryGroup");
//		}
//		entry.setEntryGroup(entryGroup);
//		modelMap.addAttribute("result", entryService.save(entry));
//	}
//	
//	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE + " && #userId == authentication.principal.id")
//	@RequestMapping(method = RequestMethod.GET, value = "/{entryId}")
//	public void findOne(@PathVariable long userId, @PathVariable long entryId, Authentication authentication, ModelMap modelMap) {
//		log.debug("modelMap : {}", modelMap);
//		modelMap.addAttribute("result", entryService.findOne(entryId));
//	}
//	
//	/**
//	 * double entry의 수정에 대한 처리 필요
//	 * @param userId
//	 * @param entry
//	 * @param authentication
//	 * @param modelMap
//	 */
//	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE + " && #userId == authentication.principal.id")
//	@RequestMapping(value = "/{entryId}", method = RequestMethod.PUT)
//	public void modify(@PathVariable long userId, @RequestBody Entry entry, Authentication authentication, ModelMap modelMap) {
//		Entry targetEntry = entryService.findOne(entry.getId());
//		
//		if (!targetEntry.getAsset().getUsername().equals(authentication.getName())) {
//			throw new BlueskyException("username is not owner");
//		}
//		
//		if (entry.getAsset() == null || entry.getAsset().getId() <= 0) {
//			throw new BlueskyException("asset is empty");
//		}
//		if (entry.getEntryGroup() == null || entry.getEntryGroup().getId() <= 0) {
//			throw new BlueskyException("assetGroup is empty");
//		}
//		Asset asset = assetService.findOne(entry.getAsset().getId());
//		if (!asset.getUsername().equals(authentication.getName())) {
//			throw new BlueskyException("not owner's asset");
//		}
//		targetEntry.setAsset(asset);
//		
//		EntryGroup entryGroup = entryGroupService.findOne(entry.getEntryGroup().getId());
//		if (!entryGroup.getUsername().equals(authentication.getName())) {
//			throw new BlueskyException("not owner's entryGroup");
//		}
//		targetEntry.setEntryGroup(entryGroup);
//		
//		if (targetEntry.isTransferEntry()) {
//			//금액 변경시 동일 이슈 처리, 삭제시도 동일 이슈 처리
//		}
//		
//		
//		targetEntry.setAmount(entry.getAmount());
//		targetEntry.setDate(entry.getDate());
//		targetEntry.setMemo(entry.getMemo());
//		
//		modelMap.addAttribute("result", entryService.save(targetEntry));
//	}
//	
//	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE + " && #userId == authentication.principal.id")
//	@RequestMapping(value = "/{entryId}", method = RequestMethod.DELETE)
//	public void delete(@PathVariable long userId, @PathVariable long entryId, Authentication authentication, ModelMap modelMap) {
//		Entry targetEntry = entryService.findOne(entryId);
//		if (!authentication.getName().equals(targetEntry.getAsset().getUsername())) {
//			throw new BlueskyException("username is not owner");
//		}
//		log.debug("id : {}", entryId);
//		entryService.delete(entryId);
//	}
}
