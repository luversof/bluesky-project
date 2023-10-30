package net.luversof.web.gate.vaadin.bookkeeping.layout;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;

import net.luversof.web.gate.vaadin.layout.CommonLayout;

@RoutePrefix("bookkeeping")
@ParentLayout(CommonLayout.class)
public class BookkeepingLayout extends Div implements RouterLayout {
	
	private static final long serialVersionUID = 1L;
	
	public BookkeepingLayout() {
		getStyle().set("border", "1px solid red");
//		getStyle().set("margin-top", "20px");
		
		createBottomMenu();
	}

	
	public void createBottomMenu() {
		// 특정 조건시엔 안보이게 해야 할 수도 있을까?
		var tabs = new Tabs();
		tabs.add(new Tab("TEST111"));
		tabs.add(new Tab("TEST222"));
		tabs.add(new Tab("TEST333"));
		tabs.addThemeVariants(TabsVariant.LUMO_MINIMAL,
                TabsVariant.LUMO_EQUAL_WIDTH_TABS);
		add(tabs);
	}
}
