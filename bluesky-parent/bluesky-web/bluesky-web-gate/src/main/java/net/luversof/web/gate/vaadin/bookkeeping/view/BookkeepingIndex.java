package net.luversof.web.gate.vaadin.bookkeeping.view;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import net.luversof.web.gate.bookkeeping.client.BookkeepingClient;
import net.luversof.web.gate.user.util.UserUtil;
import net.luversof.web.gate.vaadin.GateVaadin;
import net.luversof.web.gate.vaadin.bookkeeping.layout.BookkeepingLayout;
import net.luversof.web.gate.vaadin.bookkeeping.util.BookkeepingVaadinUtil;

@RolesAllowed("ROLE_USER")
@Route(value = "", layout = BookkeepingLayout.class)
public class BookkeepingIndex extends HorizontalLayout implements GateVaadin {

	private static final long serialVersionUID = 1L;
	
	private BookkeepingClient bookkeepingClient;

	public BookkeepingIndex(BookkeepingClient bookkeepingClient) {
		this.bookkeepingClient = bookkeepingClient;
		
	}

	@Override
	public void createView() {
		var bookkeepingList = bookkeepingClient.findByUserId(UserUtil.getUserId());
		// 로그인한 유저의 bookkeeping으로 forward 처리
		if (bookkeepingList.isEmpty()) {
			BookkeepingVaadinUtil.moveToBookkeepingCreate();
		} else {
			// bookkeeping 목록 보여줌
			for (var bookkeeping : bookkeepingList) {
				add(new Span(bookkeeping.getName()));
			}
		}
		
		// 없으면 생성 화면으로 forward 처리
	}
	
	

}
