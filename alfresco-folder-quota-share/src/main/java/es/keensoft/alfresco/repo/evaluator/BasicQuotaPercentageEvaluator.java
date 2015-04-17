package es.keensoft.alfresco.repo.evaluator;

import org.alfresco.web.evaluator.BaseEvaluator;
import org.json.simple.JSONObject;

public abstract class BasicQuotaPercentageEvaluator extends BaseEvaluator {
	
	public Long getPercentage(JSONObject json) {
		
		JSONObject node = (JSONObject) json.get("node");
		JSONObject properties = (JSONObject) node.get("properties");
		
		Double current = Double.parseDouble(properties.get("fq:sizeCurrent").toString());
		Double quota = Double.parseDouble(properties.get("fq:sizeQuota").toString());
		
		return Math.round((1 - (current / quota)) * 100);
		
	}
	
}