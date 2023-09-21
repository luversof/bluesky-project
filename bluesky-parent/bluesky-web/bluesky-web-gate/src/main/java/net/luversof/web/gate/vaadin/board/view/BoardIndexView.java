package net.luversof.web.gate.vaadin.board.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import jakarta.annotation.security.RolesAllowed;
import net.luversof.web.gate.vaadin.layout.CommonLayout;

//@RolesAllowed("ROLE_USER")
@AnonymousAllowed
@PageTitle("Board")
@Route(value = "/board", layout = CommonLayout.class)
public class BoardIndexView extends HorizontalLayout {

	private static final long serialVersionUID = 1L;

	public BoardIndexView() {
		add(new RouterLink("List", BoardListView.class, new RouteParameters("boardAlias", "free")));
    }
    

}
