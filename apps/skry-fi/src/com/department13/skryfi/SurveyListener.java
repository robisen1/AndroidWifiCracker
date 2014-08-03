package com.department13.skryfi;

import java.util.Map;

public interface SurveyListener {
	/** called when a scan completes with a list of visible networks */
	public void survey_event(SurveyManager manager, Map<String, Network> networks);

}
