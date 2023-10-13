package net.luversof.web.gate.vaadin.bookkeeping.view;

import org.springframework.util.StringUtils;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.RolesAllowed;
import net.luversof.web.gate.bookkeeping.client.BookkeepingClient;
import net.luversof.web.gate.bookkeeping.domain.Bookkeeping;
import net.luversof.web.gate.vaadin.GateVaadin;
import net.luversof.web.gate.vaadin.bookkeeping.layout.BookkeepingLayout;
import net.luversof.web.gate.vaadin.bookkeeping.util.BookkeepingVaadinUtil;

@RolesAllowed("ROLE_USER")
@Route(value = "create", layout = BookkeepingLayout.class)
public class BookkeepingCreate extends FormLayout implements GateVaadin {
	
	private static final long serialVersionUID = 1L;

	private BookkeepingClient bookkeepingClient;
	
	
	private Bookkeeping bookkeeping;
	private Binder<Bookkeeping> binder = new Binder<>(Bookkeeping.class);
	
	private TextField nameField = new TextField();
	private IntegerField baseDateField = new IntegerField(); 
	
	
	private Button createButton = new Button();
	
	public BookkeepingCreate(BookkeepingClient bookkeepingClient) {
		this.bookkeepingClient = bookkeepingClient;
	}

	@Override
	public void updateLocale() {
		nameField.setLabel(getTranslation("bookkeeping.name"));
		baseDateField.setLabel(getTranslation("bookkeeping.baseDate"));
		
		createButton.setText(getTranslation("bookkeeping.button.create"));
	}

	@Override
	public void createView() {
		
		baseDateField.setMin(1);
		baseDateField.setMax(28);
		
		bookkeeping = new Bookkeeping();
		binder.readBean(bookkeeping);
		binder.forField(nameField).withValidator(StringUtils::hasText, (valueContext) -> getTranslation("bookkeeing.name.validate")).bind(Bookkeeping::getName, Bookkeeping::setName);
		binder.forField(baseDateField).withValidator(x -> x != null, (valueContext) -> getTranslation("bookkeeing.baseDate.validate")).bind(Bookkeeping::getBaseDate, Bookkeeping::setBaseDate);
		
		add(nameField);
		add(baseDateField);
		
		createButton.addClickListener(e -> {
			try {
				binder.writeBean(bookkeeping);
			} catch (ValidationException e1) {
				e1.printStackTrace();
			}
			
			var result = bookkeepingClient.create(bookkeeping);
			BookkeepingVaadinUtil.moveToBookkeepingEntry(result.getBookkeepingId());
		});
		
		add(createButton);
	}
	
	

}
