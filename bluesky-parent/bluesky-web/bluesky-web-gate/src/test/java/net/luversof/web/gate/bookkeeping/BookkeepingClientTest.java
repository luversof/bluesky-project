package net.luversof.web.gate.bookkeeping;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import net.luversof.GeneralWebTest;
import net.luversof.web.gate.bookkeeping.client.BookkeepingClient;
import net.luversof.web.gate.bookkeeping.domain.Bookkeeping;

class BookkeepingClientTest implements GeneralWebTest {

	@Autowired
	private BookkeepingClient bookkeepingClient;
	
	@Test
	void beanCheck() {
		assertThat(bookkeepingClient).isNotNull();
	}
	
	
	@Test
	void findByUserId() {
		List<Bookkeeping> bookkeepingList = bookkeepingClient.findByUserId("string");
		assertThat(bookkeepingList).isNotEmpty();
	}
}
