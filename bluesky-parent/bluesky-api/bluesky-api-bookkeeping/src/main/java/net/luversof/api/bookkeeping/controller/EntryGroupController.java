//package net.luversof.api.bookkeeping.controller;
//
//import java.util.List;
//import java.util.UUID;
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
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//import net.luversof.api.bookkeeping.domain.EntryGroup;
//import net.luversof.api.bookkeeping.service.CompositeEntryGroupService;
//
//@RestController
//@RequestMapping(value = "/api/bookkeeping/entryGroup", produces = MediaType.APPLICATION_JSON_VALUE)
//public class EntryGroupController {
//
//	@Autowired
//	private CompositeEntryGroupService entryGroupService;
//	
//	@PostMapping
//	public EntryGroup create(@RequestBody @Validated(EntryGroup.Create.class) EntryGroup entryGroup) {
//		return entryGroupService.create(entryGroup);
//	}
//
//	@GetMapping
//	public List<EntryGroup> findByBookkeepingId(@RequestParam UUID bookkeepingId) {
//		return entryGroupService.findByBookkeepingId(bookkeepingId);
//	}
//
//	@PutMapping
//	public EntryGroup update(@RequestBody @Validated(EntryGroup.Update.class) EntryGroup entryGroup) {
//		return entryGroupService.update(entryGroup);
//	}
//
//	@DeleteMapping
//	public void delete(@RequestBody @Validated(EntryGroup.Delete.class) EntryGroup entryGroup) {
//		entryGroupService.delete(entryGroup);
//	}
//
//}
