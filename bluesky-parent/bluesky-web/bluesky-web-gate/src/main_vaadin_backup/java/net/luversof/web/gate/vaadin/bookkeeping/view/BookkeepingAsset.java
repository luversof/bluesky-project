package net.luversof.web.gate.vaadin.bookkeeping.view;

import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import net.luversof.web.gate.vaadin.GateVaadin;
import net.luversof.web.gate.vaadin.bookkeeping.layout.BookkeepingLayout;

@RolesAllowed("ROLE_USER")
@Route(value = ":bookkeepingId/asset", layout = BookkeepingLayout.class)
public class BookkeepingAsset extends FormLayout implements GateVaadin {
	
	private static final long serialVersionUID = 1L;

	@Override
	public void createView() {
		add(new Span("asset 보기"));
	}

	
}
