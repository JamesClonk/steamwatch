package models

import play.api.data._
import play.api.data.Forms._

case class Setting(
  defaultSalesPercentage: Int,
  wishlists: List[String])

object Setting {
  val form = Form(
    mapping(
      "defaultSalesPercentage" -> number,
      "wishlist" -> list(text))(Setting.apply)(Setting.unapply))
}