package ch.jamesclonk.steamwatch.model

import scala.reflect.BeanProperty
import org.htmlcleaner.HtmlCleaner
import org.htmlcleaner.DomSerializer
import org.htmlcleaner.TagNode
import scala.xml.XML
import org.htmlcleaner.PrettyXmlSerializer
import dispatch._
import scala.xml.Elem
import java.sql.Timestamp
import org.slf4j.Logger
import com.typesafe.config.{ Config, ConfigFactory }

object SteamData {

  val config = ConfigFactory.load()
  private val cleaner = new HtmlCleaner
  private val props = cleaner.getProperties

  private lazy val steamUrlId = config.getString("steam.url.id")
  private lazy val steamUrlProfile = config.getString("steam.url.profile")

  def requestWishlistData(wishlist: Wishlist): Option[Elem] = {
    val steamUrl = if (wishlist.id.matches("^[0-9]{5}[0-9]*$")) steamUrlProfile else steamUrlId
    val request = url(steamUrl.replaceAll(":WISHLIST:", wishlist.id))
    val response = Http(request OK as.String)

    val node = cleaner.clean(response())
    val tagNodes = node.evaluateXPath("//div[@id='wishlist_items']").map(e => e.asInstanceOf[TagNode])

    // serialize to scala.xml.Elem
    if (!tagNodes.isEmpty && tagNodes.size > 0) {
      Some(XML.loadString((new PrettyXmlSerializer(props)).getAsString(tagNodes.head, "utf-8")))
    } else {
      None
    }
  }

  def nvl(in: String, out: String): String =
    if (in == null || in == "") out
    else in

  def getAppsFromWishlist(wishlist: Wishlist): Option[List[App]] = {
    requestWishlistData(wishlist) match {
      case Some(xml) => {
        val apps = xml \ "div"
        val result = apps.map { app =>
          val appId = (app \ "@id").text.substring(5)

          val urls = (app \\ "a")(0)
          val storeUrl = (urls \ "@href").text
          val logoUrl = (urls \\ "@src").text

          val name = (app \\ "h4").text

          val prices = (app \ "div" \ "div" \ "div")(1)
          val priceType = (prices \ "@class").text
          val price = if (priceType == "price") {
            val price = nvl(prices.text.replaceAll("[^\\d]", ""), "5000").toInt
            (price, price)
          } else {
            val normalPrice = nvl(((prices \\ "div") filter (e => (e \\ "@class").text == "discount_original_price")).text.replaceAll("[^\\d]", ""), "5000").toInt
            val discountPrices = nvl(((prices \\ "div") filter (e => (e \\ "@class").text == "discount_final_price")).text.replaceAll("[^\\d]", ""), "5000").toInt
            (normalPrice, discountPrices)
          }

          App(
            appId.toInt,
            name,
            logoUrl,
            storeUrl,
            new Timestamp(System.currentTimeMillis),
            price._1,
            price._2,
            new Timestamp(System.currentTimeMillis))
        }

        Some(result.toList)
      }
      case None => None
    }
  }

}