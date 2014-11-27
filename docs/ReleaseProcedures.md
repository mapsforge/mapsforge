# Release Procedures 

This documents the steps for an official mapsforge release.

Set Version Numbers:
--------------------
Set the new version number with

mvn versions:set -DnewVersion=0.5.0

and edit 
build.gradle
Applications/Android/Samples/AndroidManifest.xml


Release to Maven Central:
-------------------------

mvn -DperformRelease=true -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Dhttps.protocols=SSLv3 -Dforce.http.jre.executor=true -DskipTests=true  -s settings.xml deploy

or (if there is no ssl bug)

mvn -DperformRelease=true -Dforce.http.jre.executor=true -DskipTests=true  -s settings.xml deploy


The settings.xml file needs to look like this:

<settings>
  <servers>
    <server>
      <id>ossrh</id>
      <username>secretusername</username>
      <password>secretpassword</password>
    </server>
  </servers>
</settings>


(Ludwig has the secretusername and secretpassword for this).

This step seems to have the habit of losing certain elements (like jars that do not get uploaded). The publishing step performed by sonatype should find those and complain.)

Publish Release
---------------

Log into 
https://oss.sonatype.org/index.html

The user is Jürgen, Ludwig has id and password for it.

and navigate to Staging Repositories, there should be then a mapsforge one somewhere in the list (not sure if there is an easier way to find things).

Check the released artifacts and click on Close. Sonatype will then perform its own validation. After that click Release, at that point it will be staged
to maven central, but might take some time. 


This is where the sona type user was created for mapsforge
https://issues.sonatype.org/browse/OSSRH-4231

Git tags
--------
Add tags to Git commit in form 0.5.0 

Jenkins
-------
Add a jenkins build based on the Git tag (copy previous release and just change tag to check out)

Devoloper Links
---------------
Add release information to the developer wikis.

