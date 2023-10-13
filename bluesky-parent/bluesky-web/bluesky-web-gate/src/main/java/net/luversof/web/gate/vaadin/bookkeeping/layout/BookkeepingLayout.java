package net.luversof.web.gate.vaadin.bookkeeping.layout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;

import net.luversof.web.gate.vaadin.layout.CommonLayout;

@RoutePrefix("bookkeeping")
@ParentLayout(CommonLayout.class)
public class BookkeepingLayout extends Div implements RouterLayout {
	
	private static final long serialVersionUID = 1L;

}
