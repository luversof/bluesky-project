package net.luversof.web.gate.vaadin.board.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Article;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.gate.board.client.BoardArticleClient;
import net.luversof.web.gate.board.domain.BoardArticle;
import net.luversof.web.gate.vaadin.GateVaadin;
import net.luversof.web.gate.vaadin.board.layout.BoardLayout;
import net.luversof.web.gate.vaadin.board.util.BoardVaadinUtil;

@AnonymousAllowed
@Route(value = ":boardAlias/view/:boardArticleId", layout = BoardLayout.class)
public class BoardArticleView extends VerticalLayout implements GateVaadin {
	
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
	}

	@Override
	public void setInstanceVariable(BeforeEnterEvent event) {
		boardAlias = event.getRouteParameters().get("boardAlias").get();
		boardArticleId = event.getRouteParameters().get("boardArticleId").get();
		boardArticle = boardArticleClient.findByBoardArticleId(boardArticleId).orElseThrow(() -> new BlueskyException("board.NOT_EXIST_BOARDARTICLE"));
	}
	
	@Override
	public void updateLocale() {
		listButton.setText(getTranslation("board.button.list"));
		editButton.setText(getTranslation("board.button.edit"));
		writeButton.setText(getTranslation("board.button.write"));
	}
	
	@Override
	public void createView() {
		var article = new Article();
		article.add(new H1(boardArticle.getTitle()));
		article.add(new Paragraph(boardArticle.getContent()));
		
		listButton.addClickListener(e -> BoardVaadinUtil.moveToList(boardAlias));
		editButton.addClickListener(e -> BoardVaadinUtil.moveToEdit(boardAlias, boardArticleId));
		writeButton.addClickListener(e -> BoardVaadinUtil.moveToWrite(boardAlias));
		
		var buttonArea = new HorizontalLayout();
		buttonArea.add(listButton, editButton, writeButton);
		
		add(article, buttonArea);
	}
}
