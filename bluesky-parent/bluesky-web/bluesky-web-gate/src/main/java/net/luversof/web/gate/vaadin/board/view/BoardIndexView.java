package net.luversof.web.gate.vaadin.board.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import net.luversof.web.gate.vaadin.layout.CommonLayout;

@RolesAllowed("ROLE_USER")
@PageTitle("Board")
@Route(value = "/board", layout = CommonLayout.class)
public class BoardIndexView extends HorizontalLayout {

	private static final long serialVersionUID = 1L;

	public BoardIndexView() {
    	Button button = new Button("Board Index");
    	HorizontalLayout layout = new HorizontalLayout(button, new DatePicker("Pick a date"));

    	layout.setDefaultVerticalComponentAlignment(Alignment.END);
    	add(layout);
    }
    

}
