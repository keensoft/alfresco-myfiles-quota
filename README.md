alfresco-myfiles-quota
======================

This extension adds the ability to define and apply a default quota on My Files folder when a new user is created.

Alfresco's default quota is applied to every content uploaded by a user. This extension applies quota only to personal space (user home folder).

The default quota value is set in ```alfresco-global.properties``` file and must be defined in bytes.

The plugin is licensed under the [LGPL v3.0](http://www.gnu.org/licenses/lgpl-3.0.html). The current version is compatible with **5.0**.

It has been developed based on [alfresco-folder-quota](https://code.google.com/p/alfresco-folder-quota/)

On addition, during [Alfresco Global Virtual Hack-a-thon 2015](https://wiki.alfresco.com/wiki/Projects_and_Teams_Global_Virtual_Hack-a-thon_2015), folder quota setting has been included in Share interface. So every folder can be controlled by a quota (even Sites folders).

![alfresco-folder-quota](https://cloud.githubusercontent.com/assets/1818300/7205928/2fa66e04-e52d-11e4-9c5e-e057f80500f7.png)

Downloading the ready-to-deploy-plugin
--------------------------------------
The binary distribution is made of two AMP files:

* [repo AMP](https://github.com/keensoft/alfresco-myfiles-quota/releases/download/2.0.0/alfresco-folder-quota-repo-1.0.0.amp)
* [share AMP](https://github.com/keensoft/alfresco-myfiles-quota/releases/download/2.0.0/alfresco-folder-quota-share-1.0.0.amp)

You can install it by using standard [Alfresco deployment tools](http://docs.alfresco.com/community/tasks/dev-extensions-tutorials-simple-module-install-amp.html)

Building the artifacts
----------------------
If you are new to Alfresco and the Alfresco Maven SDK, you should start by reading [Jeff Potts' tutorial on the subject](http://ecmarchitect.com/alfresco-developer-series-tutorials/maven-sdk/tutorial/tutorial.html).

You can build the artifacts from source code using maven
```sh
$ mvn clean package
```

Configuring the quota
---------------------
Include and set the following properties at ```alfresco-global.properties``` on your Alfresco installation:
```sh
	# Folder quota in bytes (10 Mb)
	user.creation.default.quota=10737418
	folder.quota.core.pool.size=10
	folder.quota.maximum.pool.size=20
	folder.quota.thread.priority=5
```

All users shall be created with default quota assignment.

You can get quota for a user on Home Folder invoking the webscript: 
```sh
curl -v -u admin:admin "http://localhost:8080/alfresco/service/keensoft/myfiles/quota/{username}"
```

You can create or change quota for a user on Home Folder invoking the webscript:
```sh
curl -v -u admin:admin -H 'Content-Type: application/json' -d '{"quota":"104857600"}' "http://localhost:8080/alfresco/service/keensoft/myfiles/quota/{username}"
```
The following property must be set: quota (amount of bytes allowed in My Files user folder for user {username})
