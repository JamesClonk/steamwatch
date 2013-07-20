package ch.jamesclonk.steamwatch

import org.slf4j.Logger
import java.net.URL
import java.sql.Timestamp
import org.squeryl.PrimitiveTypeMode._
import ch.jamesclonk.steamwatch.model._

trait Update
    extends Mail {

  val logger: Logger

  def cleanupUsers(): Unit = {
    inTransaction {
      logger.info("delete all expired, still unvalidated users..")
      Data.deleteExpiredUnvalidatedUsers()
    }
  }

  def cleanupWishlists(): Unit = {
    inTransaction {
      logger.info("delete all unused wishlists..")
      Data.deleteUnusedWishlists()
      logger.info("delete all empty wishlists..")
      Data.deleteEmptyWishlists()
      logger.info("delete all orphan users-wishlists-apps..")
      Data.deleteOrphanUsersWishlistsApps
      logger.info("delete all unused users-wishlists-apps..")
      Data.deleteUnusedUsersWishlistsApps()
    }
  }

  def updateWishlists(): Unit = {
    inTransaction {
      logger.info("update all wishlists..")

      val wishlists = Data.wishlists
      wishlists foreach { wishlist =>
        updateWishlist(wishlist)
      }
    }
  }

  def updateWishlist(wishlist: Wishlist): Unit = {
    inTransaction {
      logger.info("update wishlist[" + wishlist.id + "]")

      SteamData.getAppsFromWishlist(wishlist) match {
        case Some(apps) => {
          apps foreach (_.upsert())

          // figure out which apps are not / should not be in this wishlist anymore
          wishlist.apps.filterNot(a1 => apps.exists(a2 => a1 == a2)) foreach { app =>
            logger.debug("remove app [" + app.id + "] from wishlist [" + wishlist.id + "]")
            wishlist.apps.dissociate(app) // also unlink app from wishlist

            // also maintain USERS_WISHLISTS_APPS table
            Data.deleteUserWishlistApp(wishlist.id, app.id)
          }

          // add all new apps to wishlist
          apps.filterNot(a2 => wishlist.apps.exists(a1 => a1 == a2)) foreach { app =>
            logger.debug("add app [" + app.id + "] to wishlist [" + wishlist.id + "]")
            app.wishlists.associate(wishlist) // link new app to wishlist

            // also add new entries to USERS_WISHLISTS_APPS table
            wishlist.users foreach { user =>
              val uwa = UserWishlistApp(user.id, wishlist.id, app.id, user.defaultPercentage, true, None)
              logger.debug("insert [" + uwa + "]")
              Data.usersWishlistsApps.insert(uwa)
            }
          }
        }
        case None => logger.error("wishlist[" + wishlist.id + "] returned no valid data!")
      }

    }
  }

  def updateUserWishlists(user: User, wishlists: List[String]): Unit = {
    inTransaction {
      logger.info("update wishlists of user[" + user.email + "]..")

      user.wishlists.dissociateAll
      wishlists.foreach { wishlistId =>
        Data.getWishlist(wishlistId) match {
          // upsert and update wishlist data
          case None if (wishlistId.length > 2) => {
            val wishlist = Wishlist(wishlistId,
              new Timestamp(System.currentTimeMillis),
              new Timestamp(System.currentTimeMillis))
            Data.wishlists.insert(wishlist)
            user.wishlists.associate(wishlist)
            updateWishlist(wishlist)
          }

          // link wishlist and add new entries to USERS_WISHLISTS_APPS table
          case Some(wishlist) => {
            user.wishlists.associate(wishlist)

            val appsInUwa = Data.getUserWishlistApp(user.id, wishlistId).map(uwa => Data.getApp(uwa.appId).get)
            wishlist.apps.filterNot(a2 => appsInUwa.exists(a1 => a1 == a2)) foreach { app =>
              UserWishlistApp(user.id, wishlist.id, app.id, user.defaultPercentage, true, None).upsert()
            }
          }

          case _ => // do nothing
        }
      }
    }
  }

  def checkForNotifications(): Unit = {
    inTransaction {
      logger.info("check if a user needs to be notified..")

      val notifiable = Data.getNotifiableUsersWishlistsApps()
      val users = notifiable.map(_.user.single).toList.distinct

      users foreach { user =>

        val result = notifiable.filter(_.userId == user.id) map { uwa =>
          val app = uwa.app.single
          val wishlist = uwa.wishlist.single
          (wishlist, app)
        }

        // transform result into Map[Wishlist, List[App]] and call sendMail with it..
        logger.info(" ..send notification email to [" + user.email + "]")
        sendMail(user.email, result.groupBy(e => e._1).map(e => (e._1, e._2.map(_._2))))
      }

      // set alert flag to false after successful email..
      notifiable foreach { uwa =>
        uwa.alert = false
        uwa.alertDate = Some(new Timestamp(System.currentTimeMillis))
        uwa.upsert()
      }
    }
  }

  def resetDatabaseWithInitialUser(user: User): Unit = {
    inTransaction {
      resetDatabase()
      Data.users.insert(user)
    }
  }

  def resetDatabase(): Unit = {
    inTransaction {
      Data.drop
      Data.create
    }
  }
}