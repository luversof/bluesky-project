package net.luversof.web.gate.vaadin.error.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import io.github.luversof.boot.exception.BlueskyException;
import jakarta.servlet.http.HttpServletResponse;
import net.luversof.web.gate.vaadin.layout.CommonLayout;

@AnonymousAllowed
@ParentLayout(CommonLayout.class)
@Tag(Tag.DIV)
public class BlueskyExceptionErrorView extends Component implements HasErrorParameter<BlueskyException> {

	private static final long serialVersionUID = 1L;
	
	@Override
	public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<BlueskyException> parameter) {
		
		NativeLabel label = new NativeLabel(parameter.getException().getErrorCode());
        getElement().appendChild(label.getElement());
		
		return HttpServletResponse.SC_NOT_FOUND;
	}

}
