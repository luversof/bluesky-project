package net.luversof.web.gate.vaadin.main.view;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import net.luversof.web.gate.vaadin.layout.CommonLayout;

@AnonymousAllowed
@PageTitle("Main")
@Route(value = "", layout = CommonLayout.class)
public class MainIndex extends HorizontalLayout {
	private static final long serialVersionUID = 1L;

	public MainIndex() {
    	Div div = new Div();
    	div.setText("왜 안되는거임?");
    	add(div);
    }
    

}
