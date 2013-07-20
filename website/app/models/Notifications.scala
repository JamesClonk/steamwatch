package models

import play.api.data._
import play.api.data.Forms._

case class Notification(
  wishlistId: String,
  appId: Int,
  appName: String,
  logo: String,
  storeLink: String,
  createdPrice: Int,
  currentPrice: Int,
  salesPercentage: Int,
  notifyFlag: Option[String]) {
  val alertFlag: Boolean = notifyFlag match {
    case None => false
    case Some(text) if text == "on" => true
  }
}

case class Wishlist(wishlistId: String, notifications: List[Notification])

case class Wishlists(wishlists: List[Wishlist])

object Notification {
  val form = Form(
    mapping(
      "wishlist" -> list(
        mapping(
          "wishlistId" -> nonEmptyText,
          "notifications" -> list(
            mapping(
              "wishlistId" -> nonEmptyText,
              "appId" -> number,
              "appName" -> nonEmptyText,
              "logo" -> text,
              "storeLink" -> nonEmptyText,
              "createdPrice" -> number,
              "currentPrice" -> number,
              "salesPercentage" -> number(min = 10, max = 90),
              "notifyFlag" -> optional(text))
              (Notification.apply)(Notification.unapply)))
          (Wishlist.apply)(Wishlist.unapply)))
      (Wishlists.apply)(Wishlists.unapply))
}

