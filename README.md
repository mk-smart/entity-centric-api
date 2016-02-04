# MK:Smart entity-centric API

This is a standalone server of a RESTful API for exposing integrated linked data.

This implementation is in Java and entirely Maven-based. There are no external dependency files bundled with the project, but you need to be online for Maven to download them and assemble the API.

## Requirements

To __build__ the API you will need JDK 1.7 and [Maven 3](http://maven.apache.org). It still has to be tested with Java 8.

To __run__ the API you will need a JRE 1.7 and a running instance of [CouchDB](http://couchdb.apache.org) 1.6.x.

## Building the API

From the root source directory, it's just

    $ mvn install

Since there are still some unit tests that rely upon the availability of online resources (yes I know... sorry, I'll get rid of them eventually), it may sometimes be convenient to skip tests. To do so, use

    $ mvn install -Dmaven.test.skip=true

## Running the API

There is a standalone JAR distribution with an embedded Jetty server. After building the project, go to the output directory

    $ cd standalone/target

There you will find a file called `ecapi.launcher-[version].jar`. You can run this JAR on the desired HTTP port (mandatory, as there is no default port).
	
    $ java -jar ecapi.launcher-[version].jar -c [config_file] -p [port]

`config_file` is a Java properties file. Supplying this is mandatory. A sample file can be found in the `src/main/resources` directory of the `standalone` project.

## Debugging the API

To use a custom log configuration, set VM parameter -Dlog4j.configuration=file:log4j.properties

To debug in Eclipse:

    $ java -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=8453,server=y,suspend=n -Dhttp.proxyHost=wwwcache.open.ac.uk -Dhttp.proxyPort=80 -Dlog4j.configuration=file:log4j.properties -jar ecapi.launcher-[version].jar -c [config_file] -p [port]
