package net.luversof.web.bookkeeping.controller;

import lombok.extern.slf4j.Slf4j;
import net.luversof.bookkeeping.domain.EntryGroup;
import net.luversof.bookkeeping.service.EntryGroupService;
import net.luversof.core.exception.BlueskyException;
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
@RequestMapping("/user/{userId}/bookkeeping/entryGroup")
public class EntryGroupController {

	@Autowired
	private EntryGroupService entryGroupService;

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE + " && #userId == authentication.principal.id")
	@RequestMapping(method = RequestMethod.GET, value = "")
	public String list(@PathVariable long userId, @RequestParam(defaultValue = "1") int page, Authentication authentication, ModelMap modelMap) {
		log.debug("modelMap : {}", modelMap);
		modelMap.addAttribute("result", entryGroupService.findByUsername(authentication.getName()));
		return "/bookkeeping/entryGroup/list";
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE + " && #userId == authentication.principal.id")
	@RequestMapping(method = RequestMethod.POST)
	public void save(@PathVariable long userId, @RequestBody EntryGroup entryGroup, Authentication authentication, ModelMap modelMap) {
		entryGroup.setUsername(authentication.getName());
		log.debug("save entryGroup : {}", entryGroup);
		modelMap.addAttribute("result", entryGroupService.save(entryGroup));
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE + " && #userId == authentication.principal.id")
	@RequestMapping(value = "/{entryGroupId}", method = RequestMethod.PUT)
	public void modify(@PathVariable long userId, @RequestBody EntryGroup entryGroup, Authentication authentication, ModelMap modelMap) {
		EntryGroup targetEntryGroup = entryGroupService.findOne(entryGroup.getId());
		if (!authentication.getName().equals(targetEntryGroup.getUsername())) {
			throw new BlueskyException("username is not owner");
		}
		targetEntryGroup.setName(entryGroup.getName());
		modelMap.addAttribute("result", entryGroupService.save(targetEntryGroup));
	}

	@PreAuthorize(AuthorizeRole.PRE_AUTHORIZE_ROLE + " && #userId == authentication.principal.id")
	@RequestMapping(value = "/{entryGroupId}", method = RequestMethod.DELETE)
	public void delete(@PathVariable long userId, @PathVariable long entryGroupId, Authentication authentication, ModelMap modelMap) {
		EntryGroup targetEntryGroup = entryGroupService.findOne(entryGroupId);
		if (!authentication.getName().equals(targetEntryGroup.getUsername())) {
			throw new BlueskyException("username is not owner");
		}
		log.debug("id : {}", entryGroupId);
		entryGroupService.delete(entryGroupId);
	}

}
