package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import org.squeryl.PrimitiveTypeMode._
import models._
import ch.jamesclonk.steamwatch.model.{ Data, UserWishlistApp }
import org.slf4j.Logger
import org.slf4j.helpers.NOPLogger

object Application
  extends Controller
  with Secured {
  
  val logger = NOPLogger.NOP_LOGGER

  def index = withAuthUser { user =>
    implicit request =>
      val data = getDataByUser(user.id)
      Ok(views.html.index(data, user))
  }

  def saveData = withAuthUser { user =>
    implicit request =>
      Notification.form.bindFromRequest.fold(
//        errors => {
//          BadRequest("Oops! A strange error happened... Sorry!")
//        },
        formWithErrors => BadRequest(views.html.index(formWithErrors.get, user)),
        data => {
          // update back into db
          transaction {
            data.wishlists.foreach { wishlist =>
              wishlist.notifications.foreach { notification =>
                UserWishlistApp(
                  user.id,
                  wishlist.wishlistId,
                  notification.appId,
                  notification.salesPercentage,
                  notification.alertFlag,
                  None).upsert()
              }
            }
          }

          Ok(views.html.index(data, user))
        })
  }

  private def getDataByUser(userId: Int): Wishlists = {
    transaction {
      val user = Data.getUser(userId).get
      val wishlists = user.wishlists.toList

      val result = wishlists.map { wishlist =>
        val uwa = Data.getUserWishlistApp(user.id, wishlist.id)
        val result = uwa.map { uwa =>
          val app = uwa.app.single
          Notification(
            wishlist.id,
            app.id,
            app.name,
            app.logo,
            app.storeLink,
            app.createdPrice.toInt,
            app.currentPrice.toInt,
            uwa.alertPercentage.toInt,
            if (uwa.alert) Some("on") else None)
        }

        Wishlist(wishlist.id, result)
      }

      Wishlists(result)
    }
  }

}
