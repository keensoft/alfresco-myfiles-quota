package es.keensoft.alfresco.action.webscript;

import java.io.IOException;
import java.io.Serializable;

import org.alfresco.extension.folderquota.FolderQuotaModel;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.json.simple.JSONObject;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.http.MediaType;

public class MyFilesQuotaGet extends AbstractWebScript {
	
	private PersonService personService;
	private NodeService nodeService;

	@SuppressWarnings("unchecked")
	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		
		Serializable quota = "-1";
		Serializable currentSize = "";
		
		String username = req.getExtensionPath();
		
		if (username == null || username.equals("")) {
			throw new WebScriptException("User name is required!");
		}
		
		NodeRef userNodeRef = personService.getPerson(username, false);
		NodeRef homeFolderNode = (NodeRef) nodeService.getProperty(userNodeRef, ContentModel.PROP_HOMEFOLDER);
		if (nodeService.hasAspect(homeFolderNode, FolderQuotaModel.ASPECT_FQ_QUOTA)) {
			quota = nodeService.getProperty(homeFolderNode, FolderQuotaModel.PROP_FQ_SIZE_QUOTA);
			currentSize = nodeService.getProperty(homeFolderNode, FolderQuotaModel.PROP_FQ_SIZE_CURRENT);
		}
		
    	JSONObject objProcess = new JSONObject();
    	objProcess.put("quota", quota);
    	objProcess.put("currentSize", currentSize);
		
    	String jsonString = objProcess.toString();
    	res.setContentType(MediaType.APPLICATION_JSON.toString());
    	res.setContentEncoding("UTF-8");
    	res.getWriter().write(jsonString);
    	
	}

	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

}