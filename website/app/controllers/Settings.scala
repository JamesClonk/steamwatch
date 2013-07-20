package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import org.squeryl.PrimitiveTypeMode._
import models._
import ch.jamesclonk.steamwatch.model.{ Data, User, Wishlist, App, UserWishlistApp }
import ch.jamesclonk.steamwatch.Update
import java.sql.Timestamp
import org.slf4j.Logger
import org.slf4j.helpers.NOPLogger

object Settings
  extends Controller
  with Secured 
  with Update {
  
  val logger = NOPLogger.NOP_LOGGER
  val config = null

  def index = withAuthUser { user =>
    implicit request =>
      val settings = getSettingsByUser(user)
      Ok(views.html.settings(settings, user))
  }

  def saveData = withAuthUser { user =>
    implicit request =>
      Setting.form.bindFromRequest.fold(
        formWithErrors => BadRequest(views.html.settings(formWithErrors.get, user)),
        data => {
          // update back into db
          transaction {
            if (data.defaultSalesPercentage >= 10 && data.defaultSalesPercentage <= 90) {
              user.defaultPercentage = data.defaultSalesPercentage.toDouble
              user.upsert()
            }

            updateUserWishlists(user, data.wishlists)
            cleanupWishlists()
          }

          Ok(views.html.settings(data, user))
        })
  }

  private def getSettingsByUser(user: User): Setting = {
    transaction {
      val wishlists = user.wishlists.toList.map(w => w.id)
      Setting(user.defaultPercentage.toInt, wishlists)
    }
  }
}