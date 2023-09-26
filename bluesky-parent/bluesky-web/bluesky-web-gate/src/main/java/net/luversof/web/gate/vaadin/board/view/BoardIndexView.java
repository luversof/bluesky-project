package net.luversof.web.gate.vaadin.board.view;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import net.luversof.web.gate.vaadin.board.layout.BoardLayout;

//@RolesAllowed("ROLE_USER")
@AnonymousAllowed
@PageTitle("Board")
@Route(value = "", layout = BoardLayout.class)
public class BoardIndexView extends HorizontalLayout {

	private static final long serialVersionUID = 1L;

	public BoardIndexView() {
		add(new RouterLink("List", BoardListView.class, new RouteParameters("boardAlias", "free")));
    }
    

}
