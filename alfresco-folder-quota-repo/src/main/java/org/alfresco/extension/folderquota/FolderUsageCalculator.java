package org.alfresco.extension.folderquota;

import java.util.Iterator;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FolderUsageCalculator {

	private final String query = "ASPECT:\"fq:quota\"";
	
	private static Log logger = LogFactory.getLog(FolderUsageCalculator.class);
	
	SearchService searchService;
	NodeService nodeService;
	FileFolderService fileFolderService;
	
	public void recalculate() {
		
		SearchParameters params = new SearchParameters();
		params.setLanguage(SearchService.LANGUAGE_LUCENE);
		params.setQuery(query);
		
		ResultSet rs = searchService.query(params);
		Iterator<ResultSetRow> it = rs.iterator();
		while(it.hasNext()) {
			ResultSetRow row = it.next();
			long size = calculateFolderSize(row.getNodeRef());
			nodeService.setProperty(row.getNodeRef(), FolderQuotaModel.PROP_FQ_SIZE_CURRENT, size);
		}
	}
	
	public Long calculateFolderSize(NodeRef nodeRef) {
		
		Long size = 0L;
		List<FileInfo> children = fileFolderService.list(nodeRef);
		Iterator<FileInfo> it = children.iterator();
		
		while(it.hasNext()) {
			FileInfo fi = it.next();
			
			QName nodeType = nodeService.getType(fi.getNodeRef());
			if(nodeType.isMatch(ContentModel.TYPE_FOLDER))
			{
				size = size + calculateFolderSize(fi.getNodeRef());
			}
			else
			{
				ContentData contentRef = (ContentData) nodeService.getProperty(fi.getNodeRef(), ContentModel.PROP_CONTENT);
				// #7 (nodes without content)
				if (contentRef != null) {
				    size = size + contentRef.getSize();
				}
			}
		}
		
		return size;
	}
	
    public NodeRef getParentFolderWithQuota(NodeRef nodeRef) {
    	
    	if (nodeRef == null || !nodeService.exists(nodeRef)) {
    		return null;
    	}
    	
	    if (nodeService.hasAspect(nodeRef, FolderQuotaModel.ASPECT_FQ_QUOTA)) {
	    	return nodeRef;
	    }
    	
    	ChildAssociationRef parentFolderRef = nodeService.getPrimaryParent(nodeRef);
    	if (parentFolderRef == null) {
    		return null;
    	}
    	
    	return getParentFolderWithQuota(parentFolderRef.getParentRef());
    }
    
	public Long getChangeSize(NodeRef changed) {
		
		Long change = 0L;
		
		if (changed.getStoreRef().getProtocol().equalsIgnoreCase(StoreRef.PROTOCOL_ARCHIVE)) return 0L;
		
		QName nodeType = nodeService.getType(changed);
		if (nodeType.isMatch(ContentModel.TYPE_FOLDER)) {
			change = calculateFolderSize(changed);
		} else {
			
    		if (nodeService.exists(changed)) {
	    		ContentData contentRef = (ContentData) nodeService.getProperty(changed, ContentModel.PROP_CONTENT);
	    		if(contentRef != null) {
	    			change = contentRef.getSize();
	    		}
    		} else {
    			logger.warn("A node was deleted from a quota folder and was not available for size calculations, folder usage reporting may be inaccurate");
    		}
    		
		}
		
		return change;
	}
	
	public String getQuery() {
		return query;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setFileFolderService(FileFolderService fileFolderService) {
		this.fileFolderService = fileFolderService;
	}
	
}
