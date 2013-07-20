package ch.jamesclonk.steamwatch.model

import java.sql.Timestamp
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.slf4j.Logger
import org.slf4j.helpers.NOPLogger
import com.typesafe.config.{ Config, ConfigFactory }

@RunWith(classOf[JUnitRunner])
class SteamDataTestSuite
    extends FunSuite
    with BeforeAndAfter {

  test("requestWishlistData") {
    val wishlist = Wishlist("jamesclonk",
      new Timestamp(System.currentTimeMillis),
      new Timestamp(System.currentTimeMillis))

    val xmlOption = SteamData.requestWishlistData(wishlist)
    assert(xmlOption.isDefined)

    val xml = xmlOption.get
    assert(xml.isInstanceOf[scala.xml.Elem])
    assert(!xml.isEmpty)
  }

  test("getAppsFromWishlist") {
    val wishlist = Wishlist("jamesclonk",
      new Timestamp(System.currentTimeMillis),
      new Timestamp(System.currentTimeMillis))

    val appsOption = SteamData.getAppsFromWishlist(wishlist)
    assert(appsOption.isDefined)

    val apps = appsOption.get
    assert(!apps.isEmpty)
    assert(apps.size > 10)
    assert(apps.size < 70)
    assert(apps.exists(app => app.name == "Stacking"))
    assert(!apps.exists(app => app.name == "Chantelise"))
  }

}