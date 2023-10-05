package net.luversof.web.gate.vaadin.board.view;

import org.springframework.util.StringUtils;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;

import io.github.luversof.boot.exception.BlueskyException;
import jakarta.annotation.security.RolesAllowed;
import net.luversof.web.gate.board.client.BoardArticleClient;
import net.luversof.web.gate.board.client.BoardClient;
import net.luversof.web.gate.board.domain.BoardArticle;
import net.luversof.web.gate.user.util.UserUtil;
import net.luversof.web.gate.vaadin.GateVaadin;
import net.luversof.web.gate.vaadin.board.layout.BoardLayout;

@RolesAllowed("ROLE_USER")
@Route(value = ":boardAlias/edit/:boardArticleId", layout = BoardLayout.class)
public class BoardArticleEdit extends FormLayout implements GateVaadin {

	private static final long serialVersionUID = 1L;
	
	private BoardClient boardClient;
	
	private BoardArticleClient boardArticleClient;
	
	private String boardAlias;
	private String boardArticleId;
	
	private BoardArticle boardArticle;
	
	private Binder<BoardArticle> binder = new Binder<>(BoardArticle.class);
	
	private TextField titleField = new TextField();
	private TextArea contentTextArea = new TextArea();
	
	
	private Button editButton = new Button();
	private Button cancelButton = new Button();
	
	public BoardArticleEdit(BoardClient boardClient, BoardArticleClient boardArticleClient) {
		this.boardClient = boardClient;
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
		titleField.setLabel(getTranslation("boardArticle.title"));
		titleField.setPlaceholder(getTranslation("boardArticle.title.placeholder"));
		
		contentTextArea.setLabel(getTranslation("boardArticle.content"));
		contentTextArea.setPlaceholder(getTranslation("boardArticle.content.placeholder"));
		
		
		editButton.setText(getTranslation("board.button.edit"));
		cancelButton.setText(getTranslation("board.button.cancel"));
	}
	
	@Override
	public void createView() {
		updateLocale();
		
		binder.forField(titleField).withValidator(StringUtils::hasText, (valueContext) -> getTranslation("boardArticle.title.validate")).bind(BoardArticle::getTitle, BoardArticle::setTitle);
		binder.forField(contentTextArea).withValidator(StringUtils::hasText, (valueContext) -> getTranslation("boardArticle.content.validate")).bind(BoardArticle::getContent, BoardArticle::setContent);
		binder.readBean(boardArticle);
		
		setResponsiveSteps(
			new ResponsiveStep("0", 1)
		);
		
		add(titleField);
		add(contentTextArea);
		
		editButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		editButton.addClickListener(e -> {
			try {
				binder.writeBean(boardArticle);
			} catch (ValidationException e1) {
				e1.printStackTrace();
			}
			
			var board = boardClient.findByAlias(boardAlias);
			
			boardArticle.setUserId(UserUtil.getUserId());
			boardArticle.setBoardId(board.boardId());
			var result = boardArticleClient.modify(boardArticle);
			
			UI.getCurrent().navigate(BoardArticleView.class, new RouteParameters(new RouteParam("boardAlias", boardAlias), new RouteParam("boardArticleId", result.getBoardArticleId())));
			
			System.out.println("resuilt : " +  boardArticle.getBoardArticleId());
		});
		
		var buttonLayout = new HorizontalLayout(editButton, cancelButton);
		buttonLayout.setJustifyContentMode(JustifyContentMode.CENTER);
		
		add(buttonLayout);
		
	}


}
