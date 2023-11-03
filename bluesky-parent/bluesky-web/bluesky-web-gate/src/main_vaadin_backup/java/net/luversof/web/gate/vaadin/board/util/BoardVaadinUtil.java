package net.luversof.web.gate.vaadin.board.util;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.RouteParam;
import com.vaadin.flow.router.RouteParameters;

import lombok.experimental.UtilityClass;
import net.luversof.web.gate.vaadin.board.view.BoardArticleEdit;
import net.luversof.web.gate.vaadin.board.view.BoardArticleList;
import net.luversof.web.gate.vaadin.board.view.BoardArticleView;
import net.luversof.web.gate.vaadin.board.view.BoardArticleWrite;

@UtilityClass
public class BoardVaadinUtil {

	public static void moveToWrite(String boardAlias) {
		UI.getCurrent().navigate(BoardArticleWrite.class, new RouteParameters("boardAlias", boardAlias));
	}
	
	public static void moveToView(String boardAlias, String boardArticleId) {
		UI.getCurrent().navigate(BoardArticleView.class, new RouteParameters(new RouteParam("boardAlias", boardAlias), new RouteParam("boardArticleId", boardArticleId)));
	}
	
	public static void moveToEdit(String boardAlias, String boardArticleId) {
		UI.getCurrent().navigate(BoardArticleEdit.class, new RouteParameters(new RouteParam("boardAlias", boardAlias), new RouteParam("boardArticleId", boardArticleId)));
	}
	
	public static void moveToList(String boardAlias) {
		UI.getCurrent().navigate(BoardArticleList.class, new RouteParameters("boardAlias", boardAlias));
	}
	
}
