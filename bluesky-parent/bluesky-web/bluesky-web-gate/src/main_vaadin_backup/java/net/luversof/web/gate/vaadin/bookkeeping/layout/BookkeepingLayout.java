package net.luversof.web.gate.vaadin.bookkeeping.layout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabVariant;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;

import jakarta.annotation.security.RolesAllowed;
import net.luversof.web.gate.bookkeeping.client.BookkeepingClient;
import net.luversof.web.gate.bookkeeping.domain.Bookkeeping;
import net.luversof.web.gate.user.util.UserUtil;
import net.luversof.web.gate.vaadin.GateVaadin;
import net.luversof.web.gate.vaadin.bookkeeping.util.BookkeepingVaadinUtil;
import net.luversof.web.gate.vaadin.layout.CommonLayout;

@RoutePrefix("bookkeeping")
@ParentLayout(CommonLayout.class)
public class BookkeepingLayout extends Div implements RouterLayout, GateVaadin {
	
	private static final long serialVersionUID = 1L;
	
	private BookkeepingClient bookkeepingClient;
	private String bookkeepingId;
	private Bookkeeping bookkeeping;
	
	private Span entrySpan = new Span();
	private Span statisticsSpan = new Span();
	private Span assetSpan = new Span();
	private Span configSpan = new Span();
	
	private Tab entryTab = new Tab(VaadinIcon.USER.create(), entrySpan);
	private Tab statisticsTab = new Tab(VaadinIcon.ACCESSIBILITY.create(), statisticsSpan);
	private Tab assetTab = new Tab(VaadinIcon.ADOBE_FLASH.create(), assetSpan);
	private Tab configTab = new Tab(VaadinIcon.ANCHOR.create(), configSpan);
	
	private Tab[] tabs = new Tab[] { entryTab, statisticsTab, assetTab, configTab };
	private Tabs bookkeepingTabs = new Tabs(tabs);
	
	public BookkeepingLayout(BookkeepingClient bookkeepingClient) {
		this.bookkeepingClient = bookkeepingClient;
		
		getStyle().set("border", "1px solid red");
//		getStyle().set("margin-top", "20px");
	}

	@Override
	public void updateLocale() {
		entrySpan.setText("입력");
		statisticsSpan.setText("통계2");
		assetSpan.setText("자산");
		configSpan.setText("설정");
	}
	
	@Override
	public void setInstanceVariable(BeforeEnterEvent event) {
		bookkeepingId = event.getRouteParameters().get("bookkeepingId").orElse(null);
		
		if (bookkeepingId != null  && UserUtil.getLoginInfo().isLogin()) {
			var userBoookkeepingList = bookkeepingClient.findByUserId(UserUtil.getUserId());
			bookkeeping = userBoookkeepingList.stream().filter(x -> x.getBookkeepingId().equals(bookkeepingId)).findAny().orElse(null);
		}
	}
	
	@Override
	public void createView() {
		if (getComponentCount() > 0) {
			return;
		}
		
//		entrySpan.setSizeFull();
////		entrySpan.addClickListener(e -> BookkeepingVaadinUtil.moveToBookkeepingEntry("test"));
//		statisticsSpan.setSizeFull();
//		assetSpan.setSizeFull();
//		configSpan.setSizeFull();
		
		// 특정 조건시엔 안보이게 해야 할 수도 있을까?
//		Tab entryTab = new Tab(entryButton);
		
//		tabs.addThemeVariants(TabsVariant.LUMO_MINIMAL,
//				TabsVariant.LUMO_EQUAL_WIDTH_TABS);
		
		for (var tab : tabs) {
			tab.addThemeVariants(TabVariant.LUMO_ICON_ON_TOP);
		}
		
		bookkeepingTabs.addThemeVariants(TabsVariant.LUMO_EQUAL_WIDTH_TABS);
		
		bookkeepingTabs.addAttachListener(e -> {
			// 처음 뭐 select 하는지 처리하면 되나?
		});
		
		bookkeepingTabs.addSelectedChangeListener(e -> {
			if (e.getSelectedTab() == entryTab) {
				BookkeepingVaadinUtil.moveToEntry("test");
				return;
			} else if (e.getSelectedTab() == statisticsTab) {
				
			}
		});
		
//		bookkeepingTabs.setSelectedTab(entryTab);
		add(bookkeepingTabs);
	}
}
