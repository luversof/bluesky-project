package net.luversof.web.gate.vaadin.main.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import net.luversof.web.gate.vaadin.layout.CommonLayout;

@PageTitle("Main")
@Route(value = "", layout = CommonLayout.class)
public class IndexView extends HorizontalLayout {

	private static final long serialVersionUID = 1L;

	public IndexView() {
    	Button button = new Button("도대체 뭐가 문제임?");
    	HorizontalLayout layout = new HorizontalLayout(button, new DatePicker("Pick a date"));

    	layout.setDefaultVerticalComponentAlignment(Alignment.END);
    	add(layout);
    }
    

}
