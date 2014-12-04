package org.alfresco.extension.folderquota;

import org.alfresco.service.namespace.QName;

public interface FolderQuotaModel {
	
    public static final String FOLDER_QUOTA_MODEL_PREFIX = "fq";
    public static final String FOLDER_QUOTA_MODEL_1_0_URI = "http://www.alfresco.org/model/folder-quota/1.0";

    public static final QName ASPECT_FQ_QUOTA = QName.createQName(FOLDER_QUOTA_MODEL_1_0_URI, "quota");
    public static final QName PROP_FQ_SIZE_QUOTA = QName.createQName(FOLDER_QUOTA_MODEL_1_0_URI, "sizeQuota");
    public static final QName PROP_FQ_SIZE_CURRENT = QName.createQName(FOLDER_QUOTA_MODEL_1_0_URI, "sizeCurrent");

}
