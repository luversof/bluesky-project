package net.luversof.web.gate.vaadin.bookkeeping.util;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.RouteParameters;

import lombok.experimental.UtilityClass;
import net.luversof.web.gate.vaadin.bookkeeping.view.BookkeepingAsset;
import net.luversof.web.gate.vaadin.bookkeeping.view.BookkeepingCreate;
import net.luversof.web.gate.vaadin.bookkeeping.view.BookkeepingEntry;
import net.luversof.web.gate.vaadin.bookkeeping.view.BookkeepingStatistics;

/**
 * view page 구성
 * 가계부 목록 : BookkeepingIndex
 * 가계부 생성 : BookkeepingCreate
 * 
 * 가계부 입력 화면 : BookkeepingEntry
 */
@UtilityClass
public class BookkeepingVaadinUtil {

	public static void moveToBookkeepingCreate() {
		UI.getCurrent().navigate(BookkeepingCreate.class);
	}
	
	public static void moveToEntry(String bookkeepingId) {
		UI.getCurrent().navigate(BookkeepingEntry.class, new RouteParameters("bookkeepingId", bookkeepingId));
	}
	
	public static void moveToStatistics(String bookkeepingId) {
		UI.getCurrent().navigate(BookkeepingStatistics.class, new RouteParameters("bookkeepingId", bookkeepingId));
	}
	
	public static void moveToAsset(String bookkeepingId) {
		UI.getCurrent().navigate(BookkeepingAsset.class, new RouteParameters("bookkeepingId", bookkeepingId));
	}
}
