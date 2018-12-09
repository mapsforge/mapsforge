### Desktop

To run the desktop samples can use the Gradle `run` task.

Can change the [mainClassName](../mapsforge-samples-awt/build.gradle) to run each sample and pass args:
```groovy
./gradlew :mapsforge-samples-awt:run -Pargs=/path/to/map
```

To create a standalone executable jar, adapt the main class in [build gradle](../mapsforge-samples-awt/build.gradle), then run:
```groovy
./gradlew :mapsforge-samples-awt:fatJar
```
The jar file can be found in `build/libs` folder. Depending on the main class, pass args on execution via command line:
```
java -jar mapsforge-samples-awt-master-SNAPSHOT-jar-with-dependencies.jar /path/to/map
```
