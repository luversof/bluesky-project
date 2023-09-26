package net.luversof.web.gate.vaadin.board.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;

import net.luversof.web.gate.board.client.BoardArticleClient;
import net.luversof.web.gate.board.domain.BoardArticle;
import net.luversof.web.gate.vaadin.board.layout.BoardLayout;

@AnonymousAllowed
@Route(value = ":boardAlias/list", layout = BoardLayout.class)
public class BoardListView extends VerticalLayout implements BeforeEnterObserver {
	
	private static final long serialVersionUID = 1L;
	
	private BoardArticleClient boardArticleClient;
	
	private String boardAlias;
	
	public BoardListView(BoardArticleClient boardArticleClient) {
		this.boardArticleClient = boardArticleClient;
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		boardAlias = event.getRouteParameters().get("boardAlias").get();
		createView();
	}
	
	void createView() {
		
		var boardArticleGrid = new Grid<>(BoardArticle.class);
		boardArticleGrid.setColumns("id", "title", "createdDate");
		boardArticleGrid.addColumn(e -> "테스트 : " + e.getUserId()).setId("testColumn");
		
		boardArticleGrid.setItems(q -> boardArticleClient.findByBoardAlias(boardAlias, VaadinSpringDataHelpers.toSpringPageRequest(q)).stream());
		
		add(boardArticleGrid);
		
		var writeButton = new Button("글쓰기", e -> {
			
		});
		add(new RouterLink("Write", BoardWriteView.class, new RouteParameters("boardAlias", boardAlias)));
		
		add(writeButton);
		
	}
}
