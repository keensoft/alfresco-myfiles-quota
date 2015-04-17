package es.keensoft.alfresco.action.executer;

import java.util.List;

import org.alfresco.extension.folderquota.FolderQuotaModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;

public class FolderQuota extends ActionExecuterAbstractBase {
	
	private static final String PARAM_NEW_QUOTA = "size-quota";
	
	private NodeService nodeService;

	@Override
	protected void executeImpl(Action action, NodeRef nodeRef) {
		
		String newQuotaInMb = (String) action.getParameterValue(PARAM_NEW_QUOTA);
		
		if (!nodeService.hasAspect(nodeRef, FolderQuotaModel.ASPECT_FQ_QUOTA)) {
			nodeService.addAspect(nodeRef, FolderQuotaModel.ASPECT_FQ_QUOTA, null);
		}
		
		Long newQuota = Long.parseLong(newQuotaInMb) * 1024 * 1024;
		if (newQuota == 0) {
		    nodeService.removeAspect(nodeRef, FolderQuotaModel.ASPECT_FQ_QUOTA);	
		} else {
		    nodeService.setProperty(nodeRef, FolderQuotaModel.PROP_FQ_SIZE_QUOTA, newQuota);
		}

		
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		paramList.add(new ParameterDefinitionImpl(PARAM_NEW_QUOTA, DataTypeDefinition.TEXT, false,
				getParamDisplayLabel(PARAM_NEW_QUOTA)));
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

}
