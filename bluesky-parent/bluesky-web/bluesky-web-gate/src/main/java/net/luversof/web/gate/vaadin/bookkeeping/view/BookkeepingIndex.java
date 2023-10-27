package net.luversof.web.gate.vaadin.bookkeeping.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import net.luversof.web.gate.bookkeeping.client.BookkeepingClient;
import net.luversof.web.gate.bookkeeping.domain.Bookkeeping;
import net.luversof.web.gate.user.util.UserUtil;
import net.luversof.web.gate.vaadin.GateVaadin;
import net.luversof.web.gate.vaadin.bookkeeping.layout.BookkeepingLayout;
import net.luversof.web.gate.vaadin.bookkeeping.util.BookkeepingVaadinUtil;

@RolesAllowed("ROLE_USER")
@Route(value = "", layout = BookkeepingLayout.class)
public class BookkeepingIndex extends HorizontalLayout implements GateVaadin {

	private static final long serialVersionUID = 1L;
	
	private BookkeepingClient bookkeepingClient;
	
	private Button createButton = new Button();

	public BookkeepingIndex(BookkeepingClient bookkeepingClient) {
		System.out.println("TEST : " + getComponentCount());
		//removeAll();
		this.bookkeepingClient = bookkeepingClient;
	}
	
	

	@Override
	public void updateLocale() {
		createButton.setText(getTranslation("bookkeeping.button.create"));
	}

	@Override
	public void createView() {
		if (getComponentCount() > 0) {
			return;
		}
		
		// bookkeeping 목록 보여줌
		var bookkeepingGrid = new Grid<>(Bookkeeping.class);
		bookkeepingGrid.setColumns("name");
		bookkeepingGrid.setItems(DataProvider.ofCollection(bookkeepingClient.findByUserId(UserUtil.getUserId())));
		bookkeepingGrid.addItemClickListener(e -> BookkeepingVaadinUtil.moveToBookkeepingEntry(e.getItem().getBookkeepingId()));
		
		add(bookkeepingGrid);

		// 로그인한 유저의 bookkeeping으로 forward 처리
		createButton.addClickListener(e -> {
			BookkeepingVaadinUtil.moveToBookkeepingCreate();
		});
		
		add(createButton);
	}
	
	

}
