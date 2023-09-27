package net.luversof.web.gate.vaadin.board.view;

import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.gate.board.client.BoardArticleClient;
import net.luversof.web.gate.board.domain.BoardArticle;
import net.luversof.web.gate.vaadin.board.layout.BoardLayout;

@AnonymousAllowed
@Route(value = ":boardAlias/view/:boardArticleId", layout = BoardLayout.class)
public class BoardArticleView extends VerticalLayout implements BeforeEnterObserver {
	
	private static final long serialVersionUID = 1L;

	private BoardArticleClient boardArticleClient;
	
	private String boardAlias;
	private String boardArticleId;
	
	private BoardArticle boardArticle;
	

	public BoardArticleView(BoardArticleClient boardArticleClient) {
		this.boardArticleClient = boardArticleClient;
		add(new Span("보기 페이지"));
	}

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		boardAlias = event.getRouteParameters().get("boardAlias").get();
		boardArticleId = event.getRouteParameters().get("boardArticleId").get();
		boardArticle = boardArticleClient.findByBoardArticleId(boardArticleId).orElseThrow(() -> new BlueskyException("board.NOT_EXIST_BOARDARTICLE"));
	}
	
	void createView() {
		var article = new Article();
		article.add(new H1(boardArticle.getTitle()));
		article.add(new Paragraph(boardArticle.getContent()));
		
		add(article);
	}
}
