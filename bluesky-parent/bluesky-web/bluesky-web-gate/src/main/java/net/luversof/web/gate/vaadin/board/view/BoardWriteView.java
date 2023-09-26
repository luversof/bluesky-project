package net.luversof.web.gate.vaadin.board.view;

import org.springframework.util.StringUtils;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.i18n.LocaleChangeEvent;
import com.vaadin.flow.i18n.LocaleChangeObserver;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import net.luversof.web.gate.board.client.BoardArticleClient;
import net.luversof.web.gate.board.client.BoardClient;
import net.luversof.web.gate.board.domain.BoardArticle;
import net.luversof.web.gate.user.util.UserUtil;
import net.luversof.web.gate.vaadin.board.layout.BoardLayout;

@RolesAllowed("ROLE_USER")
@Route(value = ":boardAlias/write", layout = BoardLayout.class)
public class BoardWriteView extends FormLayout implements BeforeEnterObserver, LocaleChangeObserver  {

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
	
	public BoardWriteView(BoardClient boardClient, BoardArticleClient boardArticleClient) {
		this.boardClient = boardClient;
		this.boardArticleClient = boardArticleClient;
	}
	
	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		boardAlias = event.getRouteParameters().get("boardAlias").get();
		createView();
	}

	@Override
	public void localeChange(LocaleChangeEvent event) {
		updateLocale();
	}
	
	void createView() {
		updateLocale();
		
		boardArticle = new BoardArticle();
		binder.readBean(boardArticle);
		binder.forField(titleField).withValidator(StringUtils::hasText, (valueContext) -> getTranslation("boardArticle.title.validate")).bind(BoardArticle::getTitle, BoardArticle::setTitle);
		binder.forField(contentTextArea).withValidator(StringUtils::hasText, (valueContext) -> getTranslation("boardArticle.content.validate")).bind(BoardArticle::getContent, BoardArticle::setContent);
		
		setResponsiveSteps(
			new ResponsiveStep("0", 1)
		);
		
		add(titleField);
		add(contentTextArea);
		
		writeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
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
			
			System.out.println("resuilt : " +  boardArticle.getBoardArticleId());
		});
		
		var buttonLayout = new HorizontalLayout(writeButton, cancelButton);
		buttonLayout.setJustifyContentMode(JustifyContentMode.CENTER);
		
		add(buttonLayout);
		
	}
	
	void updateLocale() {
		titleField.setLabel(getTranslation("boardArticle.title"));
		titleField.setPlaceholder(getTranslation("boardArticle.title.placeholder"));
		
		contentTextArea.setLabel(getTranslation("boardArticle.content"));
		contentTextArea.setPlaceholder(getTranslation("boardArticle.content.placeholder"));
		
		
		writeButton.setText(getTranslation("board.button.write"));
		cancelButton.setText(getTranslation("board.button.cancel"));
	}

}
