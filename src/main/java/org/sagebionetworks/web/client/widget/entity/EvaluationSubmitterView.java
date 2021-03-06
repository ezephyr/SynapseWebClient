package org.sagebionetworks.web.client.widget.entity;

import java.util.HashSet;
import java.util.List;

import org.sagebionetworks.evaluation.model.Evaluation;
import org.sagebionetworks.repo.model.Reference;
import org.sagebionetworks.web.client.SynapseView;
import org.sagebionetworks.web.client.utils.Callback;

public interface EvaluationSubmitterView extends SynapseView {

	void setPresenter(Presenter presenter);
	void popupSelector(boolean showEntityFinder, List<Evaluation> evaluations);
	void hideWindow();
	void showSubmissionAcceptedDialogs(HashSet<String> receiptMessages);
	void showAccessRequirement(
			String arText,
			final Callback touAcceptanceCallback);
	public interface Presenter {
		//view sends back the selected entity, selected evaluation Ids (will not be empty)
		void submitToEvaluations(Reference selectedEntity, String submissionName, String teamName, List<Evaluation> evaluationIds);
	}
}
