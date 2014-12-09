package es.keensoft.alfresco.action.webscript;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.extension.folderquota.FolderQuotaModel;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.extensions.webscripts.AbstractWebScript;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.http.MediaType;

public class MyFilesQuotaPost extends AbstractWebScript {
	
	private PersonService personService;
	private NodeService nodeService;

	@SuppressWarnings("unchecked")
	@Override
	public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {
		
		String username = req.getExtensionPath();
		
		if (username == null || username.equals("")) {
			throw new WebScriptException("User name is required!");
		}
		
		NodeRef userNodeRef = personService.getPerson(username, false);
		NodeRef homeFolderNode = (NodeRef) nodeService.getProperty(userNodeRef, ContentModel.PROP_HOMEFOLDER);
		
		try {
			
			JSONObject param = (JSONObject) new JSONParser().parse(req.getContent().getContent());
			Serializable quota = (Serializable) param.get("quota");
			
			if (nodeService.hasAspect(homeFolderNode, FolderQuotaModel.ASPECT_FQ_QUOTA)) {
				nodeService.setProperty(homeFolderNode, FolderQuotaModel.PROP_FQ_SIZE_QUOTA, quota);
			} else {
				Map<QName, Serializable> aspectProperties = new HashMap<QName, Serializable>();
				aspectProperties.put(FolderQuotaModel.PROP_FQ_SIZE_QUOTA, quota);
				aspectProperties.put(FolderQuotaModel.PROP_FQ_SIZE_CURRENT, 0);
				nodeService.addAspect(homeFolderNode, FolderQuotaModel.ASPECT_FQ_QUOTA, aspectProperties);
			}
			
	    	JSONObject objProcess = new JSONObject();
	    	objProcess.put("result", "ok");
			
	    	String jsonString = objProcess.toString();
	    	res.setContentType(MediaType.APPLICATION_JSON.toString());
	    	res.setContentEncoding("UTF-8");
	    	res.getWriter().write(jsonString);
    	
		} catch (Exception e) {
			throw new IOException(e);
		}
    	
	}
	
	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

}
