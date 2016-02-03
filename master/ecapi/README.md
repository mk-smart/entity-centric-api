# MK:Smart entity-centric API

This is a Maven project. Not a single JAR file has to be manually downloaded to assemble the project and its dependencies. Yep that's right.

## Requirements

To __build__ the API you will need JDK 1.7 and [Maven 3](http://maven.apache.org). It still has to be tested with Java 8.

To __run__ the API you will need a JRE 1.7 and a running instance of [CouchDB](http://couchdb.apache.org) 1.6.x.

## Building the API

From the root source directory type

    $ mvn install

Since some unit tests rely upon the availability of online resources, it may sometimes be a good idea to skip tests. To do so, use

    $ mvn install -Dmaven.test.skip=true

## Running the API

There is a standalone JAR distribution with an embedded Jetty server. After building the project, go to the output directory

    $ cd standalone/target

There you will find a file called `ecapi.launcher-[version].jar`. You can run this JAR on the desired HTTP port.
	
    $ java -jar ecapi.launcher-[version].jar -c [config_file] -p [port]

`config_file` is a Java properties file. Supplying this is mandatory. A sample file can be found in the `src/main/resources` directory of the `standalone` project.

## Debugging the API

To use a custom log configuration, set VM parameter -Dlog4j.configuration=file:log4j.properties

To debug in Eclipse:

    $ java -Xdebug -Xnoagent -Xrunjdwp:transport=dt_socket,address=8453,server=y,suspend=n -Dhttp.proxyHost=wwwcache.open.ac.uk -Dhttp.proxyPort=80 -Dlog4j.configuration=file:log4j.properties -jar ecapi.launcher-[version].jar -c [config_file] -p [port]