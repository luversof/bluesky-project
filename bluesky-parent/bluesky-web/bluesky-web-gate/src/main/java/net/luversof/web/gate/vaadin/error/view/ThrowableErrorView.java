package net.luversof.web.gate.vaadin.error.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.NativeLabel;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.ParentLayout;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import jakarta.servlet.http.HttpServletResponse;
import net.luversof.web.gate.vaadin.layout.CommonLayout;

@AnonymousAllowed
@ParentLayout(CommonLayout.class)
@Tag(Tag.DIV)
public class ThrowableErrorView extends Component implements HasErrorParameter<Exception> {

	private static final long serialVersionUID = 1L;
	
	@Override
	public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<Exception> parameter) {
		
		var message = parameter.getException().getLocalizedMessage();
		
		NativeLabel label = new NativeLabel(message == null ? "Exception 에러가 발생하였습니다." : message);
        getElement().appendChild(label.getElement());
		
		return HttpServletResponse.SC_NOT_FOUND;
	}

}
