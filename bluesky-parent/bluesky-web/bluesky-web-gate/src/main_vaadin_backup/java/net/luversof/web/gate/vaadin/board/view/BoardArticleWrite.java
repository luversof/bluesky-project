package net.luversof.web.gate.vaadin.board.view;

import org.springframework.util.StringUtils;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import net.luversof.web.gate.board.client.BoardArticleClient;
import net.luversof.web.gate.board.client.BoardClient;
import net.luversof.web.gate.board.domain.BoardArticle;
import net.luversof.web.gate.user.util.UserUtil;
import net.luversof.web.gate.vaadin.GateVaadin;
import net.luversof.web.gate.vaadin.board.layout.BoardLayout;
import net.luversof.web.gate.vaadin.board.util.BoardVaadinUtil;

@RolesAllowed("ROLE_USER")
@Route(value = ":boardAlias/write", layout = BoardLayout.class)
public class BoardArticleWrite extends FormLayout implements GateVaadin {

	private static final long serialVersionUID = 1L;
	
	private BoardClient boardClient;
	
	private BoardArticleClient boardArticleClient;
	
	private String boardAlias;
	
	private BoardArticle boardArticle;
	private Binder<BoardArticle> binder = new Binder<>(BoardArticle.class);
	
	private TextField titleField = new TextField();
	private TextArea contentTextArea = new TextArea();
	
	
	private Button writeButton = new Button();
	private Button cancelButton = new Button();
	
	public BoardArticleWrite(BoardClient boardClient, BoardArticleClient boardArticleClient) {
		this.boardClient = boardClient;
		this.boardArticleClient = boardArticleClient;
	}
	
	@Override
	public void setInstanceVariable(BeforeEnterEvent event) {
		boardAlias = event.getRouteParameters().get("boardAlias").get();
	}

	@Override
	public void updateLocale() {
		titleField.setLabel(getTranslation("boardArticle.title"));
		titleField.setPlaceholder(getTranslation("boardArticle.title.placeholder"));
		
		contentTextArea.setLabel(getTranslation("boardArticle.content"));
		contentTextArea.setPlaceholder(getTranslation("boardArticle.content.placeholder"));
		
		
		writeButton.setText(getTranslation("board.button.write"));
		cancelButton.setText(getTranslation("board.button.cancel"));
	}
	
	@Override
	public void createView() {
		boardArticle = new BoardArticle();
		binder.readBean(boardArticle);
		binder.forField(titleField).withValidator(StringUtils::hasText, (valueContext) -> getTranslation("boardArticle.title.validate")).bind(BoardArticle::getTitle, BoardArticle::setTitle);
		binder.forField(contentTextArea).withValidator(StringUtils::hasText, (valueContext) -> getTranslation("boardArticle.content.validate")).bind(BoardArticle::getContent, BoardArticle::setContent);
		
		setResponsiveSteps(
			new ResponsiveStep("0", 1)
		);
		
		add(titleField);
		add(contentTextArea);
		
//		writeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
		writeButton.addClickListener(e -> {
			try {
				binder.writeBean(boardArticle);
			} catch (ValidationException e1) {
				e1.printStackTrace();
			}
			
			var board = boardClient.findByAlias(boardAlias);
			
			boardArticle.setUserId(UserUtil.getUserId());
			boardArticle.setBoardId(board.boardId());
			var result = boardArticleClient.create(boardArticle);
			
			BoardVaadinUtil.moveToView(boardAlias, result.getBoardArticleId());
		});
		
		
		cancelButton.addClickListener(e -> UI.getCurrent().getPage().getHistory().go(-1));
		
		var buttonLayout = new HorizontalLayout(writeButton, cancelButton);
		buttonLayout.setJustifyContentMode(JustifyContentMode.CENTER);
		
		add(buttonLayout);
		
	}


}
