package ch.jamesclonk.steamwatch

import java.sql.DriverManager
import org.squeryl.{ Session, SessionFactory }
import org.squeryl.adapters._
import org.squeryl.internals.DatabaseAdapter
import org.slf4j.Logger
import com.typesafe.config.Config

trait Database {

  val logger: Logger
  val config: Config

  private lazy val dbDriver = config.getString("database.driver")
  private lazy val dbUrl = config.getString("database.url")
  private lazy val dbUser = config.getString("database.user")
  private lazy val dbPassword = config.getString("database.password")

  def initDatabase() = {
    Class.forName(dbDriver)
    SessionFactory.concreteFactory = dbDriver match {
      case "org.h2.Driver" => Some(() => getSession(new H2Adapter))
      case "com.mysql.jdbc.Driver" => Some(() => getSession(new MySQLAdapter))
      case "org.postgresql.Driver" => Some(() => getSession(new PostgreSqlAdapter))
      case _ => sys.error("Database driver must be org.h2.Driver, com.mysql.jdbc.Driver or org.postgresql.Driver")
    }
  }

  private def getSession(adapter: DatabaseAdapter) =
    Session.create(DriverManager.getConnection(dbUrl, dbUser, dbPassword), adapter)

}