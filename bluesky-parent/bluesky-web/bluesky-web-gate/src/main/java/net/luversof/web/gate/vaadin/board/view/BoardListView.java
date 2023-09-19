package net.luversof.web.gate.vaadin.board.view;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import io.github.luversof.boot.exception.BlueskyException;
import net.luversof.web.gate.vaadin.layout.CommonLayout;

@AnonymousAllowed
@Route(value = "/board/:boardId/list", layout = CommonLayout.class)
public class BoardListView extends HorizontalLayout implements BeforeEnterObserver {
	
	private static final long serialVersionUID = 1L;
	
	private String boardId;

	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		boardId = event.getRouteParameters().get("boardId").get();
		
		createView();
	}
	
	void createView() {
		add(new Span("목록 보기 페이지" + boardId));
		add(new Button("테스트", e -> {
			System.out.println(boardId);
		}));
		
		if (1 == 1) {
			throw new BlueskyException("TEST.except");
		}
	}
}
