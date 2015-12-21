package org.alfresco.extension.folderquota.behaviour;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import org.alfresco.extension.folderquota.FolderQuotaModel;
import org.alfresco.extension.folderquota.FolderUsageCalculator;
import org.alfresco.extension.folderquota.SizeChange;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.ContentServicePolicies;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.repo.transaction.TransactionListener;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.usage.ContentQuotaException;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.log4j.Logger;

public class FolderQuotaBehaviour implements ContentServicePolicies.OnContentPropertyUpdatePolicy,
	NodeServicePolicies.BeforeDeleteNodePolicy, NodeServicePolicies.OnMoveNodePolicy, 
	NodeServicePolicies.OnAddAspectPolicy, NodeServicePolicies.OnCreateNodePolicy,
	NodeServicePolicies.OnRestoreNodePolicy {

    private static Logger logger = Logger.getLogger(FolderQuotaBehaviour.class.getName());
    
    private PolicyComponent policyComponent;
    private NodeService nodeService;
    private TransactionService transactionService;
    private FolderUsageCalculator usage;
    private ThreadPoolExecutor threadPoolExecutor;
    private TransactionListener transactionListener;
    private Long defaultQuota;
    
    private boolean updateOnAddAspect = true;
    private static final String KEY_FOLDER_SIZE_CHANGE = FolderQuotaBehaviour.class.getName() + ".sizeUpdate";
    
    public void init() {
    	
        policyComponent.bindClassBehaviour(ContentServicePolicies.OnContentPropertyUpdatePolicy.QNAME, ContentModel.TYPE_CONTENT, 
        		new JavaBehaviour(this, "onContentPropertyUpdate", Behaviour.NotificationFrequency.TRANSACTION_COMMIT));
        policyComponent.bindClassBehaviour(NodeServicePolicies.BeforeDeleteNodePolicy.QNAME, ContentModel.TYPE_CONTENT, 
        		new JavaBehaviour(this, "beforeDeleteNode", Behaviour.NotificationFrequency.FIRST_EVENT));
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnMoveNodePolicy.QNAME, ContentModel.TYPE_CONTENT, 
        		new JavaBehaviour(this, "onMoveNode", Behaviour.NotificationFrequency.TRANSACTION_COMMIT));
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnAddAspectPolicy.QNAME, FolderQuotaModel.ASPECT_FQ_QUOTA, 
        		new JavaBehaviour(this, "onAddAspect", Behaviour.NotificationFrequency.TRANSACTION_COMMIT));
		policyComponent.bindClassBehaviour(NodeServicePolicies.OnCreateNodePolicy.QNAME, ContentModel.TYPE_PERSON, 
				new JavaBehaviour(this, "onCreateNode", NotificationFrequency.TRANSACTION_COMMIT));
        policyComponent.bindClassBehaviour(NodeServicePolicies.OnRestoreNodePolicy.QNAME, ContentModel.TYPE_CONTENT, 
        		new JavaBehaviour(this, "onRestoreNode", Behaviour.NotificationFrequency.TRANSACTION_COMMIT));

        transactionListener = new FolderSizeTransactionListener();
        
    }
    
	@Override
	public void onCreateNode(ChildAssociationRef childAssocRef) {
		
		final NodeRef user = childAssocRef.getChildRef();
		
		// Updating / creating userhome node properties requires run as system
		AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<Void>() {
			
		    @Override
		    public Void doWork() throws Exception {

				NodeRef homeFolderNode = (NodeRef) nodeService.getProperty(user, ContentModel.PROP_HOMEFOLDER);
				Map<QName, Serializable> aspectProperties = new HashMap<QName, Serializable>();
				aspectProperties.put(FolderQuotaModel.PROP_FQ_SIZE_QUOTA, defaultQuota);
				aspectProperties.put(FolderQuotaModel.PROP_FQ_SIZE_CURRENT, 0);
				nodeService.addAspect(homeFolderNode, FolderQuotaModel.ASPECT_FQ_QUOTA, aspectProperties);
				return null;
				
		    }
		    
		});
		
	}
    

	@Override
    public void onContentPropertyUpdate(final NodeRef nodeRef, QName propertyQName, ContentData beforeValue, ContentData afterValue) {
		
    	long contentSize = 0;
    	
    	if(beforeValue == null) contentSize = afterValue.getSize();
    	else contentSize = afterValue.getSize() - beforeValue.getSize();
    	
        NodeRef quotaParent = AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<NodeRef>() {
        	
        	@Override
        	public NodeRef doWork() throws Exception {
        		return usage.getParentFolderWithQuota(nodeRef);
        	}
        	
        });
        
        if(contentSize > 0) {
        	
	        if(quotaParent != null) {
	        	
	        	Long quotaSize = (Long) nodeService.getProperty(quotaParent, FolderQuotaModel.PROP_FQ_SIZE_QUOTA);
	        	Long currentSize = (Long) nodeService.getProperty(quotaParent, FolderQuotaModel.PROP_FQ_SIZE_CURRENT);
	        	
                String owner = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_OWNER);
                if ((owner == null) || (owner.equals(OwnableService.NO_OWNER))) {
                    owner = (String) nodeService.getProperty(nodeRef, ContentModel.PROP_CREATOR);
                }
	        	
	        	if (quotaSize != null) {
	        		
		        	if (currentSize + contentSize > quotaSize) {
		                throw new ContentQuotaException("User (" + owner + ") quota exceeded: content=" + contentSize +
                                ", usage=" + currentSize +
                                ", quota=" + quotaSize);
		        	} else {
		        		updateSize(quotaParent, contentSize);
		        	}
		        	
	        	} else {
	        		logger.warn(String.format("[FolderQuota] - Folder %s has the quota aspect added but no quota set", quotaParent));
	        	}
	        }
        }
		
    }

	@Override
	public void onMoveNode(final ChildAssociationRef before, final ChildAssociationRef after) {
		
        NodeRef quotaParentBefore = AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<NodeRef>() {
        	
        	@Override
        	public NodeRef doWork() throws Exception {
        		return usage.getParentFolderWithQuota(before.getParentRef());
        	}
        	
        }); 
        		
        NodeRef quotaParentAfter = AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<NodeRef>() {
        	
        	@Override
        	public NodeRef doWork() throws Exception {
        		return usage.getParentFolderWithQuota(after.getParentRef());
        	}
        	
        });  
        
        long change = 0L;
        
        if(quotaParentBefore != null || quotaParentAfter != null) {
        	change = usage.getChangeSize(before.getChildRef());
        }
        
    	if(quotaParentBefore != null) {
    		updateSize(quotaParentBefore, change * -1);
    	}
    	
    	if(quotaParentAfter != null) {
    		updateSize(quotaParentAfter, change);
    	}
    	
    	if (quotaParentAfter != null) {
			Long quotaSize = (Long) nodeService.getProperty(quotaParentAfter, FolderQuotaModel.PROP_FQ_SIZE_QUOTA);
			Long currentSize = (Long) nodeService.getProperty(quotaParentAfter, FolderQuotaModel.PROP_FQ_SIZE_CURRENT);
			if (currentSize + change > quotaSize) {
				String folderName = (String) nodeService.getProperty(quotaParentAfter, ContentModel.PROP_NAME);
		        throw new ContentQuotaException("Folder (" + folderName + ") quota exceeded: content=" + change + 
		                ", usage=" + currentSize +
		                ", quota=" + quotaSize);
			}
    	}
    	
	}
	
	@Override
	public void onRestoreNode(final ChildAssociationRef childAssocRef) {
        NodeRef quotaParent = AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<NodeRef>() {
        	
        	@Override
        	public NodeRef doWork() throws Exception {
        		return usage.getParentFolderWithQuota(childAssocRef.getChildRef());
        	}
        	
        });
        if (quotaParent != null) {
        	long change = usage.getChangeSize(childAssocRef.getChildRef());
			Long quotaSize = (Long) nodeService.getProperty(quotaParent, FolderQuotaModel.PROP_FQ_SIZE_QUOTA);
			Long currentSize = (Long) nodeService.getProperty(quotaParent, FolderQuotaModel.PROP_FQ_SIZE_CURRENT);
			if (currentSize + change > quotaSize) {
				String folderName = (String) nodeService.getProperty(quotaParent, ContentModel.PROP_NAME);
		        throw new ContentQuotaException("Folder (" + folderName + ") quota exceeded: content=" + change + 
		                ", usage=" + currentSize +
		                ", quota=" + quotaSize);
			}
        }
	}

	@Override
	public void beforeDeleteNode(final NodeRef deleted) {
		
        NodeRef quotaParent = AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<NodeRef>() {
        	
        	@Override
        	public NodeRef doWork() throws Exception {
        		return usage.getParentFolderWithQuota(deleted);
        	}
        	
        }); 
        
    	if(quotaParent != null) {
    		Long size = usage.getChangeSize(deleted);
    		updateSize(quotaParent, size * -1);
    	}	
	}
	
	@Override
	public void onAddAspect(final NodeRef nodeRef, QName aspectTypeQName) {
		
		// Accessing userhome node properties requires run as system
		AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<Void>() {
			
		    @Override
		    public Void doWork() throws Exception {
		    	
				if (nodeService.hasAspect(nodeRef, FolderQuotaModel.ASPECT_FQ_QUOTA) && updateOnAddAspect) {
					Long size = usage.calculateFolderSize(nodeRef);
					updateSize(nodeRef, size);
				}
				return null;
				
		    }
		    
		});
		
		
	}

	private void updateSize(NodeRef quotaFolder, Long sizeChange) {
		
		AlfrescoTransactionSupport.bindListener(transactionListener);
		
        @SuppressWarnings("unchecked")
		Set<SizeChange> sizeChanges = (Set<SizeChange>) AlfrescoTransactionSupport.getResource(KEY_FOLDER_SIZE_CHANGE);
        if (sizeChanges == null) {
        	sizeChanges = new HashSet<SizeChange>(10);
            AlfrescoTransactionSupport.bindResource(KEY_FOLDER_SIZE_CHANGE, sizeChanges);
        }
        sizeChanges.add(new SizeChange(quotaFolder, sizeChange));
        
	}
	
    private class FolderSizeTransactionListener extends TransactionListenerAdapter implements TransactionListener {
    	
        @Override
        public void afterCommit()
        {
            @SuppressWarnings("unchecked")
            Set<SizeChange> sizeChanges = (Set<SizeChange>) AlfrescoTransactionSupport.getResource(KEY_FOLDER_SIZE_CHANGE);
            if (sizeChanges != null)
            {
                for (SizeChange change : sizeChanges)
                {
                    Runnable runnable = new FolderSizeUpdater(change.nodeRef, change.sizeChange);
                    threadPoolExecutor.execute(runnable);
                }
            }
        }

		@Override
		public void flush() {
		}
        
    }
    
    private class FolderSizeUpdater implements Runnable {
    	
        private NodeRef quotaFolder;
        private Long sizeChange;
        private FolderSizeUpdater(NodeRef quotaFolder, Long sizeChange) {
            this.quotaFolder = quotaFolder;
            this.sizeChange = sizeChange;
        }
        
        public void run() {
        	
        	AuthenticationUtil.runAsSystem(new RunAsWork<String>() {
        		
        		public String doWork() throws Exception {
        			
        			RetryingTransactionCallback<Long> callback = new RetryingTransactionCallback<Long>() {
        				
        				public Long execute() throws Throwable {
        					Long newSize = 0L;
        					if (nodeService.exists(quotaFolder)) {
	        					Long currentSize = (Long) nodeService.getProperty(quotaFolder, FolderQuotaModel.PROP_FQ_SIZE_CURRENT);
	        					if (currentSize == null)  {
	        						currentSize=0L;
	        					}
	        					newSize = currentSize + sizeChange;
	        					if (newSize < 0) newSize = 0L;
	        					nodeService.setProperty(quotaFolder, FolderQuotaModel.PROP_FQ_SIZE_CURRENT, newSize);
        					}
        					return newSize;
        				}
        				
        			};
        			try {
        				RetryingTransactionHelper txnHelper = transactionService.getRetryingTransactionHelper();
        				txnHelper.doInTransaction(callback, false, true);

        			} catch (Throwable e) {
        				logger.error("Failed to update folder size on quota folder node: " + quotaFolder, e);
        			}
        			return "";
        		}
        	});
        }
    }

	public void setPolicyComponent(PolicyComponent policyComponent) 
	{
		this.policyComponent = policyComponent;
	}
	
	public void setFolderUsageCalculator(FolderUsageCalculator usage) 
	{
		this.usage = usage;
	}
	
	public void setUpdateUsageOnAddAspect(boolean updateOnAddAspect)
	{
		this.updateOnAddAspect = updateOnAddAspect;
	}

	public void setThreadPoolExecutor(ThreadPoolExecutor threadPoolExecutor)
	{
		this.threadPoolExecutor = threadPoolExecutor;
	}
	
	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setTransactionService(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	public void setDefaultQuota(Long defaultQuota) {
		this.defaultQuota = defaultQuota;
	}

}
