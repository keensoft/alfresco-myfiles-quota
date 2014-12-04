alfresco-myfiles-quota
======================

This extension adds the ability to define and apply a default quota on My Files folder when a new user is created.

Alfresco's default quota is applied to every content uploaded by a user. This extension applies quota only to personal space (user home folder).

The default quota value is set in ```alfresco-global.properties``` file and must be defined in bytes.

The plugin is licensed under the [LGPL v3.0](http://www.gnu.org/licenses/lgpl-3.0.html). The current version is compatible with **Alfresco 4.2.c/f** and **5.0.a/b**.

It has been developed based on [alfresco-defaultquota-policy](https://code.google.com/p/alfresco-defaultquota-policy/)

Downloading the ready-to-deploy-plugin
--------------------------------------
The binary distribution is made of one AMP file:

* [repo AMP](https://github.com/keensoft/alfresco-myfiles-quota/blob/master/dist/my-files-quota.amp?raw=true)

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
	# Folder quota in bytes
	user.creation.default.quota=10737418
	folder.quota.core.pool.size=10
	folder.quota.maximum.pool.size=20
	folder.quota.thread.priority=5
```