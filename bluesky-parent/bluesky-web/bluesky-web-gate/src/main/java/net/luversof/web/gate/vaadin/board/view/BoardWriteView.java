package net.luversof.web.gate.vaadin.board.view;

import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import net.luversof.web.gate.board.service.BoardArticleService;
import net.luversof.web.gate.vaadin.layout.CommonLayout;

@AnonymousAllowed
@Route(value = "/board/:boardAlias/write", layout = CommonLayout.class)
public class BoardWriteView extends HorizontalLayout implements BeforeEnterObserver {

	private static final long serialVersionUID = 1L;
	
	private BoardArticleService boardArticleService;
	
	private String boardAlias;
	
	public BoardWriteView(BoardArticleService boardArticleService) {
		this.boardArticleService = boardArticleService;
	}
	
	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		boardAlias = event.getRouteParameters().get("boardAlias").get();
		createView();
	}
	
	void createView() {
		add(new RouterLink("List", BoardListView.class, new RouteParameters("boardAlias", boardAlias)));	
	}
}
