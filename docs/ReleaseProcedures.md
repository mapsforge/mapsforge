# Release Procedures 

This documents the steps for an official mapsforge release.

Set Version Numbers
-------------------
Set the new version number in `build.gradle`.

Deployment to Sonatype OSSRH
----------------------------
```bash
gradlew -Dorg.gradle.parallel=false publish
```

Your `gradle.properties` needs to contain:
```
signing.keyId=
signing.password=
signing.secretKeyRingFile=

SONATYPE_USERNAME=
SONATYPE_PASSWORD=
```

Signatory credentials:
- The public key ID (an 8 character hexadecimal string)
- The passphrase used to protect your private key
- The absolute path to the secret key ring file containing your private key

Ludwig and Emux have the SONATYPE_USERNAME and SONATYPE_PASSWORD for this.

Publish Release
---------------
Log into https://oss.sonatype.org/index.html

The user is Jürgen, Ludwig and Emux have id and password for it.

And navigate to Staging Repositories, there should be then a mapsforge one somewhere in the list (not sure if there is an easier way to find things, the mapsforge one is usually at the bottom).

Check the released artifacts and click on Close. Sonatype will then perform its own validation. After that click Release, at that point it will be staged to Maven Central, but might take some time.

(This is where the Sonatype user was created for mapsforge https://issues.sonatype.org/browse/OSSRH-4231)

Git tags
--------
Add tags to Git commit in form X.Y.Z

Developer Links
---------------
Add release information to the developer wikis.
