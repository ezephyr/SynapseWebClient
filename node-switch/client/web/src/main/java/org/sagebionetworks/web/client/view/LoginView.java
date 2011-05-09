package org.sagebionetworks.web.client.view;

import org.sagebionetworks.web.shared.users.UserData;

import com.google.gwt.place.shared.Place;
import com.google.gwt.user.client.ui.IsWidget;

public interface LoginView extends IsWidget{
	
	void setPresenter(Presenter loginPresenter);

	public interface Presenter {
		void goTo(Place place);
		
		void setNewUser(UserData newUser);		
	}
}