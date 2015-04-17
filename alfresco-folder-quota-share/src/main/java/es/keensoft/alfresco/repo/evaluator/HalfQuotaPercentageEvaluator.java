package es.keensoft.alfresco.repo.evaluator;

import org.json.simple.JSONObject;

public class HalfQuotaPercentageEvaluator extends BasicQuotaPercentageEvaluator {

	@Override
	public boolean evaluate(JSONObject json) {
		
		Long percentage = super.getPercentage(json);
		return percentage >= 25 && percentage < 75;
		
	}

}
