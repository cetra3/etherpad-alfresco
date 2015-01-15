# Alfresco Etherpad integration

This Share plugin enables users to edit HTML documents within Etherpad lite from Alfresco Share.  This will create a new **Edit in Etherpad** action within the document library for HTML documents.  This allows multiple users to collaborate in real time and to save back those changes to Alfresco.

Tested with Enterprise 4.2.*, but it should work with any new-ish version of Alfresco.  Requires Dojo.

![image](etherpad-alfresco.gif)

## Installation

* On somewhere which has the JDK and Gradle installed, run `gradle jar` from the `share` directory.

* Copy `etherpad-integration-share.jar` from `share/build/libs/` into your Share's `WEB-INF/lib/` Directory

* Update your `share-config-custom.xml` to include two new endpoints, `etherpad` and `etherpad-api`.  `etherpad` is for end users and `etherpad-api` is only for Share to update content from Alfresco and setup pads.

* Update your `share-config-custom.xml` to disable CSRF actions for the etherpad endpoint, see **CSRF Instructions** below

* Restart Alfresco Share

### Example share-config-custom.xml

This example uses the following:

* 127.0.0.1 as the host
* 9001 as the port 
* an API Key of `CHANGEME`.  

To find the API key, check for the file `APIKEY.txt` on your etherpad instance.

#### Example Endpoints

```
<config evaluator="string-compare" condition="Remote">
	<remote>
		<endpoint>
			<id>etherpad</id>
			<name>Etherpad - unauthenticated access</name>
			<description>Etherpad access</description>
			<connector-id>http</connector-id>
			<endpoint-url>http://127.0.0.1:9001</endpoint-url>
			<identity>none</identity>
		</endpoint>
		<endpoint>
			<id>etherpad-api</id>
			<name>Etherpad - API access</name>
			<description>Etherpad access</description>
			<connector-id>etherpad</connector-id>
			<endpoint-url>http://127.0.0.1:9001/api/1/</endpoint-url>
			<identity>declared</identity>
			<username>apikey</username>
			<password>CHANGEME</password>
			<unsecure>true</unsecure>
		</endpoint>
	</remote>
</config>
```

#### Example CSRF Policy
```
<config evaluator="string-compare" condition="CSRFPolicy">
	<filter>
		  <rule>
            <request>
               <method>POST</method>
               <path>/proxy/etherpad/.*</path>
            </request>
         </rule>
	</filter>
</config>
```

## CSRF Instructions

Because CSRF is enabled for all POST requests, we need to override the default CSRF policy to switch it off for POST requests to the etherpad proxy.

As discussed in this [blog](http://blogs.alfresco.com/wp/ewinlof/2013/03/11/introducing-the-new-csrf-filter-in-alfresco-share/), there are two options. Both involve overriding configuration in your `share-config-custom.xml` file.

You can switch off the CSRF Policy completely by using this statement (not recommended in production):

```
<config evaluator="string-compare" condition="CSRFPolicy" replace="true">
	<filter/>
</config>
```


Or you can copy the CSRFPolicy configuration from [this file](https://svn.alfresco.com/repos/alfresco-open-mirror/alfresco/HEAD/root/projects/slingshot/config/alfresco/share-security-config.xml) and then add the example in the etherpad-alfresco readme. So you would end up with a config section like this in your share-config-custom.xml:

```
<config evaluator="string-compare" condition="CSRFPolicy">
	<filter>
		<rule>
			<request>
				<method>POST</method>
				<path>/proxy/etherpad/.*</path>
			</request>
		</rule>
		<-- OTHER RULE STATEMENTS FROM share-security-config.xml -->
	</filter>
</config>
```

What this accomplishes is that you have the default CSRFPolicy, so you are secure, but you enable POST requests to pass through unfiltered to etherpad.  You must include the etherpad filter rule first, as `share-security-config.xml` is loaded before `share-config-custom.xml`



## Todo

* Increase security of Etherpad.  Right now you will still need the Node's UUID to discover the pad, but we can do better.
* Handle error cases alot better (wrong API Key, etc..)
* Provide some documentation on setting up etherpad correctly
* Support text documents
* Test on Community and earlier versions of Enterprise