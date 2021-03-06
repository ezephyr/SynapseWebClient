package org.sagebionetworks.web.client.widget.entity.browse;

import java.util.List;

import org.sagebionetworks.repo.model.EntityHeader;
import org.sagebionetworks.web.client.SynapseView;

import com.google.gwt.user.client.ui.IsWidget;

public interface MyEntitiesBrowserView extends IsWidget, SynapseView {

	/**
	 * Set the presenter.
	 * @param presenter
	 */
	public void setPresenter(Presenter presenter);

	void setUpdatableEntities(List<EntityHeader> rootEntities);
	
	public EntityTreeBrowser getEntityTreeBrowser();
	
	void setFavoriteEntities(List<EntityHeader> favoriteEntities);

	public EntityTreeBrowser getFavoritesTreeBrowser();

	/**
	 * Presenter interface
	 */
	public interface Presenter {

		void entitySelected(String selectedEntityId);		
		
		public void loadUserUpdateable();

		void loadFavorites();
	}



}
