package net.luversof.api.bookkeeping.controller;

import java.util.UUID;
import net.luversof.api.bookkeeping.domain.Bookkeeping;
import net.luversof.api.bookkeeping.service.BookkeepingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/bookkeepings", produces = MediaType.APPLICATION_JSON_VALUE)
public class BookkeepingController {

    @Autowired private BookkeepingService bookkeepingService;

    @PostMapping
    public Bookkeeping createBookkeeping(@RequestBody Bookkeeping bookkeeping) {
        return bookkeepingService.createBookkeeping(bookkeeping);
    }

    @DeleteMapping("/byUserId")
    public void deleteBookkeepingByUserId(UUID userId) {
        bookkeepingService.deleteAllByUserId(userId);
    }

    @DeleteMapping("/byBookkeepingId")
    public void deleteBookkeepingByBookkeepingId(UUID bookkeepingId) {
        bookkeepingService.deleteAllByBookkeepingId(bookkeepingId);
    }
}
