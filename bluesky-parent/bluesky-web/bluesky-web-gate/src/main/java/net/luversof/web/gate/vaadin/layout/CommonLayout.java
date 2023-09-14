package net.luversof.web.gate.vaadin.layout;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;

import jakarta.servlet.http.Cookie;
import net.luversof.web.gate.vaadin.main.view.IndexView;

public class CommonLayout  extends AppLayout {

	private static final long serialVersionUID = 1L;

	public CommonLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("Bluesky POE");
        logo.addClassNames(
            LumoUtility.FontSize.LARGE, 
            LumoUtility.Margin.MEDIUM);

        var header = new HorizontalLayout(new DrawerToggle(), logo ); 

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER); 
        header.setWidthFull();
        header.addClassNames(
            LumoUtility.Padding.Vertical.NONE,
            LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header); 
        
        String theme = getThemeCookie();
        setTheme(theme);
        setThemeCookie(theme);
        
        var themeToggle = new Button(VaadinIcon.ADJUST.create());
        themeToggle.addClickListener(e -> {
        	String changeTheme = getThemeCookie().equals("LIGHT") ? "DARK" : "LIGHT";
        	setTheme(changeTheme);
        	setThemeCookie(changeTheme);
        });
        
        addToNavbar(themeToggle);

    }
    
    private String getThemeCookie() {
    	var cookies = VaadinService.getCurrentRequest().getCookies();
    	Cookie targetCookie = null;
    	if (cookies != null) {
    		for (var cookie : cookies) {
    			if (cookie.getName().equals("vaadinTheme")) {
    				targetCookie = cookie;
    			}
    		}
    	}
    	return targetCookie == null || targetCookie.getValue().equals("LIGHT") ?"LIGHT" : "DARK";
    }
    
    private void setThemeCookie(String theme) {
    	VaadinService.getCurrentResponse().addCookie(new Cookie("vaadinTheme", theme));
    }
    
    private void setTheme(String theme) {
    	getElement().executeJs("document.documentElement.setAttribute('theme', $0)", theme.equals("DARK") ? Lumo.DARK : Lumo.LIGHT);
    }

    private void createDrawer() {
    	var sideNav = new SideNav();
    	sideNav.addItem(
    			new SideNavItem("Index", IndexView.class, VaadinIcon.HOME_O.create()),  
//                new SideNavItem("Board", BoardIndexView.class, VaadinIcon.DATABASE.create()),
                new SideNavItem("Item2", "/ttest2", VaadinIcon.DATABASE.create())
                );
        addToDrawer(sideNav);
    }
}