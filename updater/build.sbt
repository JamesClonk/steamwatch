organization := "ch.jamesclonk.steamwatch"

name := "Updater"

version := "1.2.0"

scalaVersion := "2.9.2"

scalacOptions ++= Seq("-deprecation")

libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest" % "1.8" % "test",
    "junit" % "junit" % "4.10" % "test",
    "ch.qos.logback" % "logback-classic" % "1.0.7" % "runtime",
    "com.typesafe" % "config" % "1.0.0",
    "org.squeryl" %% "squeryl" % "0.9.5-2",
    "com.h2database" % "h2" % "1.3.170",
    "mysql" % "mysql-connector-java" % "5.1.21",
    "net.databinder.dispatch" %% "dispatch-core" % "0.9.4",
    "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.2",
    "org.apache.commons" % "commons-email" % "1.2",
    "javax.mail" % "mail" % "1.4.5",
    "org.jasypt" % "jasypt" % "1.9.0"
)

resolvers ++= Seq(
  "Sonatype OSS Releases" at "http://oss.sonatype.org/content/repositories/releases/",
  "FuseSource Releases" at "http://repo.fusesource.com/nexus/content/repositories/releases",
  "Java.net Repository" at "http://download.java.net/maven/2"
)

seq(com.github.retronym.SbtOneJar.oneJarSettings: _*)

scalariformSettings

