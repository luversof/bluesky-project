package net.luversof.web.gate.vaadin.board.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.gate.board.client.BoardArticleClient;
import net.luversof.web.gate.board.domain.BoardArticle;
import net.luversof.web.gate.vaadin.GateVaadin;
import net.luversof.web.gate.vaadin.board.layout.BoardLayout;

@AnonymousAllowed
@Route(value = ":boardAlias/view/:boardArticleId", layout = BoardLayout.class)
public class BoardArticleView extends VerticalLayout implements BeforeEnterObserver, GateVaadin {
	
	private static final long serialVersionUID = 1L;

	private BoardArticleClient boardArticleClient;
	
	private String boardAlias;
	private String boardArticleId;
	
	private BoardArticle boardArticle;
	
	private Button listButton = new Button();
	private Button editButton = new Button();
	private Button writeButton = new Button();

	public BoardArticleView(BoardArticleClient boardArticleClient) {
		this.boardArticleClient = boardArticleClient;
		add(new Span("보기 페이지"));
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		boardAlias = event.getRouteParameters().get("boardAlias").get();
		boardArticleId = event.getRouteParameters().get("boardArticleId").get();
		boardArticle = boardArticleClient.findByBoardArticleId(boardArticleId).orElseThrow(() -> new BlueskyException("board.NOT_EXIST_BOARDARTICLE"));
		
		createView();
	}
	
	@Override
	public void updateLocale() {
		listButton.setText(getTranslation("board.button.list"));
		editButton.setText(getTranslation("board.button.edit"));
		writeButton.setText(getTranslation("board.button.write"));
	}
	
	void createView() {
		var article = new Article();
		article.add(new H1(boardArticle.getTitle()));
		article.add(new Paragraph(boardArticle.getContent()));
		
		var buttonArea = new HorizontalLayout();
		
		buttonArea.add(listButton, editButton, writeButton);
		
		add(article, buttonArea);
	}
}
