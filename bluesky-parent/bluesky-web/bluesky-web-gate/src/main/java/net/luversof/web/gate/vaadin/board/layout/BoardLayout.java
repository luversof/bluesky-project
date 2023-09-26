package net.luversof.web.gate.vaadin.board.layout;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;

import net.luversof.web.gate.vaadin.layout.CommonLayout;

@RoutePrefix("board")
@ParentLayout(CommonLayout.class)
public class BoardLayout extends Div implements RouterLayout {
	
	private static final long serialVersionUID = 1L;
	
	public BoardLayout() {
	}
	
}
