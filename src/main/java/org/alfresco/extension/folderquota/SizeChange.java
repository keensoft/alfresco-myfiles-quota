package org.alfresco.extension.folderquota;

import org.alfresco.service.cmr.repository.NodeRef;

public class SizeChange {
	
	public NodeRef nodeRef;
	public long sizeChange;
	
	public SizeChange(NodeRef nodeRef,long sizeChange) {
		this.nodeRef = nodeRef;
		this.sizeChange = sizeChange;
	}
}
