package net.luversof.web.gate.vaadin.layout;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Locale;

import org.springframework.web.servlet.LocaleResolver;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.theme.lumo.Lumo;
import com.vaadin.flow.theme.lumo.LumoUtility;

import jakarta.servlet.http.Cookie;
import net.luversof.web.gate.user.util.UserUtil;
import net.luversof.web.gate.vaadin.board.view.BoardIndex;
import net.luversof.web.gate.vaadin.main.view.MainIndex;

public class CommonLayout extends AppLayout {

	private static final long serialVersionUID = 1L;
	
	private LocaleResolver localeResolver;

	public CommonLayout(LocaleResolver localeResolver) {
		this.localeResolver = localeResolver;
		
		Locale locale = localeResolver.resolveLocale(VaadinServletRequest.getCurrent().getHttpServletRequest());
		VaadinSession.getCurrent().setLocale(locale);
		System.out.println(locale.toString());
		
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        H1 logo = new H1("Bluesky Gate");
        logo.addClassNames(
            LumoUtility.FontSize.LARGE, 
            LumoUtility.Margin.MEDIUM);

        var header = new HorizontalLayout(new DrawerToggle(), logo); 

        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER); 
        header.setWidthFull();
        header.addClassNames(
            LumoUtility.Padding.Vertical.NONE,
            LumoUtility.Padding.Horizontal.MEDIUM);

        addToNavbar(header);
        
        
        var loginInfo = UserUtil.getLoginInfo();
        
        var loginInfoArea = new HorizontalLayout();
        loginInfoArea.setDefaultVerticalComponentAlignment(Alignment.CENTER);
        
        addToNavbar(loginInfoArea);
        
        if (loginInfo.isLogin()) {
        	Button logoutButton = new Button("Logout", e -> getUI().get().getPage().setLocation("/logout"));
        	loginInfoArea.add(new Span(loginInfo.getUsername()), logoutButton);
        } else {
        	
        	Button loginButton = new Button("Login", e -> getUI().get().getPage().fetchCurrentURL(url -> {
				try {
//					getUI().get().getPage().setLocation("/login?targetUrl=" + URLEncoder.encode(url.toString(), "UTF-8"));
					URLEncoder.encode(url.toString(), "UTF-8");
					getUI().get().getPage().setLocation("/login");
				} catch (UnsupportedEncodingException e1) {
					e1.printStackTrace();
				}
			}));
        	loginInfoArea.add(loginButton);
        }
        
        
        String theme = getThemeCookie();
        setTheme(theme);
        setThemeCookie(theme);
        
        var themeToggle = new Button(VaadinIcon.ADJUST.create(), e -> {
        	String changeTheme = getThemeCookie().equals("LIGHT") ? "DARK" : "LIGHT";
        	setTheme(changeTheme);
        	setThemeCookie(changeTheme);
        });
        
        loginInfoArea.add(themeToggle);
        
//        addToNavbar(themeToggle);
        
        
        I18NProvider i18nProvider = VaadinService.getCurrent().getInstantiator().getI18NProvider();
        
        Select<Locale> localeSelect = new Select<>(e -> {
        	var locale = e.getValue();
        	localeResolver.setLocale(VaadinServletRequest.getCurrent().getHttpServletRequest(), VaadinServletResponse.getCurrent().getHttpServletResponse(), locale);
        	VaadinSession.getCurrent().setLocale(locale);
        });
        localeSelect.setItems(i18nProvider.getProvidedLocales());
        localeSelect.setItemLabelGenerator(Locale::getDisplayLanguage);
        localeSelect.setValue(VaadinSession.getCurrent().getLocale());
        
        loginInfoArea.add(localeSelect);
        
//        addToNavbar(localeSelect);
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
    	
    	var boardSideNavItem = new SideNavItem("Board", BoardIndex.class, VaadinIcon.DATABASE.create());
    	boardSideNavItem.addItem(new SideNavItem("자유게시판", "board/free/list", VaadinIcon.COMMENT.create()));
//    	boardSideNavItem.addItem(new SideNavItem("BoardWrite", BoardWriteView.class));
    	
    	sideNav.addItem(
    			new SideNavItem("Home", MainIndex.class, VaadinIcon.HOME_O.create()),
    			boardSideNavItem
//                new SideNavItem("Item2", "/ttest2", VaadinIcon.DATABASE.create())
                );
        addToDrawer(sideNav);
    }
}
