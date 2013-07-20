import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "SteamWatch"
    val appVersion      = "1.0.2"

    val appDependencies = Seq(
        //"org.squeryl" % "squeryl_2.9.2" % "0.9.5-2"
        "org.squeryl" %% "squeryl" % "0.9.5-2",
        //"com.h2database" % "h2" % "1.3.170",
        "mysql" % "mysql-connector-java" % "5.1.21",
        "net.databinder.dispatch" %% "dispatch-core" % "0.9.4",
        "net.sourceforge.htmlcleaner" % "htmlcleaner" % "2.2",
        "org.apache.commons" % "commons-email" % "1.2",
        "javax.mail" % "mail" % "1.4.5",
        "org.jasypt" % "jasypt" % "1.9.0"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here      
    )

}
