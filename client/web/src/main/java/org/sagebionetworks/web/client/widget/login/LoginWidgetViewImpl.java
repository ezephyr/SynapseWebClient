package org.sagebionetworks.web.client.widget.login;

import org.sagebionetworks.web.client.IconsImageBundle;

import com.extjs.gxt.ui.client.Style.HorizontalAlignment;
import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.ButtonEvent;
import com.extjs.gxt.ui.client.event.ComponentEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.event.SelectionListener;
import com.extjs.gxt.ui.client.util.KeyNav;
import com.extjs.gxt.ui.client.widget.Label;
import com.extjs.gxt.ui.client.widget.LayoutContainer;
import com.extjs.gxt.ui.client.widget.VerticalPanel;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.extjs.gxt.ui.client.widget.form.FormButtonBinding;
import com.extjs.gxt.ui.client.widget.form.FormPanel;
import com.extjs.gxt.ui.client.widget.form.TextField;
import com.extjs.gxt.ui.client.widget.layout.FormData;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.ui.AbstractImagePrototype;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class LoginWidgetViewImpl extends LayoutContainer implements
		LoginWidgetView {

	private Presenter presenter;
	private VerticalPanel vp;
	private FormData formData;
	private Label messageLabel;
	private IconsImageBundle iconsImageBundle;
	private TextField<String> firstName = new TextField<String>();
	private TextField<String> password = new TextField<String>();

	@Inject
	public LoginWidgetViewImpl(IconsImageBundle iconsImageBundle) {
		this.iconsImageBundle = iconsImageBundle;
		messageLabel = new Label();
	}

	@Override
	protected void onRender(Element parent, int pos) {
		super.onRender(parent, pos);
		formData = new FormData("-20");
		vp = new VerticalPanel();
		vp.setSpacing(10);
		createForm1();
		add(vp);
	}

	private void createForm1() {
		FormPanel formPanel = new FormPanel();
		formPanel.setHeading("Login");
		formPanel.setFrame(true);
		formPanel.setWidth(350);
		formPanel.setLabelWidth(85);
		
		firstName.setFieldLabel("Email Address");
		firstName.setAllowBlank(false);
		firstName.getFocusSupport()
				.setPreviousId(formPanel.getButtonBar().getId());
		formPanel.add(firstName, formData);

		password.setFieldLabel("Password");
		password.setAllowBlank(false);
		password.setPassword(true);
		formPanel.add(password, formData);

		formPanel.add(messageLabel);
		
		final Button loginButton = new Button("Login", new SelectionListener<ButtonEvent>(){			
			@Override
			public void componentSelected(ButtonEvent ce) {
				messageLabel.setText(""); 
				presenter.setUsernameAndPassword(firstName.getValue(), password.getValue());
			}
		});
		formPanel.addButton(loginButton);
		formPanel.setButtonAlign(HorizontalAlignment.CENTER);
		
		FormButtonBinding binding = new FormButtonBinding(formPanel);
		binding.addButton(loginButton);

		// Enter key submits login 
		new KeyNav<ComponentEvent>(formPanel) {
			@Override
			public void onEnter(ComponentEvent ce) {
				super.onEnter(ce);
				if(loginButton.isEnabled())
					loginButton.fireEvent(Events.Select);
			}
		};
		
		vp.add(formPanel);
	}

	@Override
	public Widget asWidget() {
		return this;
	}

	@Override
	public void setPresenter(Presenter presenter) {
		this.presenter = presenter;
	}

	@Override
	public void showError(String message) {
		com.google.gwt.user.client.Window.alert(message);
	}

	@Override
	public void showAuthenticationFailed() {
		messageLabel.setStyleAttribute("color", "red");
		messageLabel.setText(AbstractImagePrototype.create(iconsImageBundle.warning16()).getHTML() + " Invalid username or password.");
		clear();
	}

	@Override
	public void clear() {
		firstName.clear();
		password.clear();
	}

}