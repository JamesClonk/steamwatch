package ch.jamesclonk.steamwatch

import org.slf4j.{ Logger, LoggerFactory }
import com.typesafe.config.{ Config, ConfigFactory }

object Main
    extends Database
    with Mail
    with Password
    with Update {

  val logger = LoggerFactory.getLogger(this.getClass)
  val config = ConfigFactory.load()

  def main(argv: Array[String]): Unit = {
    try {
      logger.info("START!")

      initDatabase()

      // reset database if commandline parameter "reset" found
      for {
        args <- Option(argv)
        arg <- args
        if arg == "reset"
      } resetDatabase()

      cleanupUsers()
      cleanupWishlists()

      updateWishlists()

      checkForNotifications()

      logger.info("DONE!")

    } catch {
      case ex: Exception => {
        logger.error(ex.toString)
        logger.error(ex.getMessage)
        logger.error(ex.getStackTrace.mkString("\n"))
        sendErrorMail(ex)
        throw ex
      }
    }
  }

}

