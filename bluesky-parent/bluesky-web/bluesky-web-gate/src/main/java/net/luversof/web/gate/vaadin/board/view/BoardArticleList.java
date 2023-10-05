package net.luversof.web.gate.vaadin.board.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.data.VaadinSpringDataHelpers;

import net.luversof.web.gate.board.client.BoardArticleClient;
import net.luversof.web.gate.board.domain.BoardArticle;
import net.luversof.web.gate.vaadin.GateVaadin;
import net.luversof.web.gate.vaadin.board.layout.BoardLayout;
import net.luversof.web.gate.vaadin.board.util.BoardVaadinUtil;

@AnonymousAllowed
@Route(value = ":boardAlias/list", layout = BoardLayout.class)
public class BoardArticleList extends VerticalLayout implements GateVaadin {
	
	private static final long serialVersionUID = 1L;
	
	private BoardArticleClient boardArticleClient;
	
	private String boardAlias;
	
	private Button writeButton = new Button();
	
	public BoardArticleList(BoardArticleClient boardArticleClient) {
		this.boardArticleClient = boardArticleClient;
	}

	@Override
	public void setInstanceVariable(BeforeEnterEvent event) {
		boardAlias = event.getRouteParameters().get("boardAlias").get();
	}
	
	@Override
	public void updateLocale() {
		writeButton.setText(getTranslation("board.button.write"));
	}
	
	@Override
	public void createView() {
		
		var boardArticleGrid = new Grid<>(BoardArticle.class);
		boardArticleGrid.setColumns("id", "title", "createdDate");
		boardArticleGrid.addColumn(e -> "테스트 : " + e.getUserId()).setId("testColumn");
		
		boardArticleGrid.setItems(q -> boardArticleClient.findByBoardAlias(boardAlias, VaadinSpringDataHelpers.toSpringPageRequest(q)).stream());
		boardArticleGrid.addItemClickListener(e -> BoardVaadinUtil.moveToView(boardAlias, e.getItem().getBoardArticleId()));
		
		add(boardArticleGrid);
		
		writeButton.addClickListener(e -> BoardVaadinUtil.moveToWrite(boardAlias));
		
		add(writeButton);
	}

}
