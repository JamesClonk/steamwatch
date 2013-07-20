package ch.jamesclonk.steamwatch

import org.squeryl.Session
import org.squeryl.adapters.H2Adapter
import org.squeryl.PrimitiveTypeMode.{ transaction, from, select }
import java.sql.DriverManager
import org.squeryl.SessionFactory
import java.sql.Timestamp
import org.scalatest.FunSuite
import org.scalatest.BeforeAndAfter
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.slf4j.Logger
import org.slf4j.helpers.NOPLogger
import com.typesafe.config.{ Config, ConfigFactory }
import ch.jamesclonk.steamwatch.model._

@RunWith(classOf[JUnitRunner])
class UpdateTestSuite
    extends FunSuite
    with BeforeAndAfter
    with Update {

  val logger = NOPLogger.NOP_LOGGER
  val config = ConfigFactory.load()
  var session: Session = null

  before {
    Class.forName("org.h2.Driver")

    SessionFactory.concreteFactory = Some(() =>
      Session.create(DriverManager.getConnection("jdbc:h2:data/steamwatch_unittest_update"), new H2Adapter))

    session = SessionFactory.newSession
    session.bindToCurrentThread

    setupDatabase()
  }

  after {
    session.cleanup
    session.close
  }

  test("Main: updateWishlists") {
    assert(wishlists.size === 1)

    def wishlist = Data.getWishlist("jamesclonk").get
    assert(wishlist.users.size === 1)
    assert(wishlist.apps.isEmpty === true)

    assert(uwa.isEmpty === true)

    updateWishlists()

    val wishlistApps = wishlist.apps.toList

    assert(uwa.isEmpty === false)

    assert(wishlistApps.size > 10)
    assert(wishlistApps.size < 100)

    assert(uwa.size === wishlistApps.size)
    assert(uwa.forall(u => wishlistApps.exists(_.appId == u.appId)), "all UWA apps exists in wishlist.apps link table")
    assert(wishlistApps.forall(a => uwa.exists(_.appId == a.id)), "all wishlist.apps exist in UWA table")
  }

  test("Main: updateUserWishlists") {
    assert(wishlists.size === 1)

    def wishlistJC = Data.getWishlist("jamesclonk")
    assert(wishlistJC.isDefined)
    assert(wishlistJC.get.users.size === 1)
    assert(wishlistJC.get.apps.isEmpty === true)

    def wishlistCl = Data.getWishlist("Clude")
    assert(wishlistCl === None)

    assert(uwa.isEmpty === true)

    def user = Data.getUser(1).get
    assert(user.email === "myuser@myuser.ch")

    updateUserWishlists(user, List("jamesclonk", "Clude"))

    assert(wishlistJC.isDefined)
    assert(wishlistCl.isDefined)

    val wishlistJCApps = wishlistJC.get.apps.toList
    val wishlistClApps = wishlistCl.get.apps.toList

    assert(uwa.isEmpty === false)

    assert(wishlistJCApps.size === 0) // why? because we do not fetch all apps anew if the wishlist already existed in db. that is updateWishlists job!
    assert(wishlistClApps.size > 1)
    assert(wishlistClApps.size < 100)

    val uwaJC = Data.getUserWishlistApp(user.id, "jamesclonk")
    val uwaCl = Data.getUserWishlistApp(user.id, "Clude")

    assert(uwaJC.size === wishlistJCApps.size)
    assert(uwaCl.size === wishlistClApps.size)

    assert(uwaJC.forall(u => wishlistJCApps.exists(_.appId == u.appId)), "all UWA JC apps exists in wishlist.apps link table")
    assert(wishlistJCApps.forall(a => uwa.exists(_.appId == a.id)), "all JC wishlist.apps exist in UWA table")
    assert(uwaCl.forall(u => wishlistClApps.exists(_.appId == u.appId)), "all UWA CL apps exists in wishlist.apps link table")
    assert(wishlistClApps.forall(a => uwa.exists(_.appId == a.id)), "all CL wishlist.apps exist in UWA table")
  }

  test("Main: updateUserWishlists - with new user") {
    updateWishlists()

    assert(wishlists.size === 1)

    val wishlist = Data.getWishlist("jamesclonk").get
    assert(wishlist.users.size === 1)
    assert(wishlist.apps.size > 10)
    assert(wishlist.apps.size < 100)

    val uwa1 = Data.getUserWishlistApp(1, "jamesclonk")
    assert(uwa1.isEmpty === false)
    assert(uwa1.size === wishlist.apps.size)

    def user = Data.getUser(2)
    assert(user === None)

    val user2 = User(2,
      "poweruser@poweruser.ch",
      "adbfadebf239930220",
      "a00a0a0beecc4432",
      new Timestamp(System.currentTimeMillis - (1 * (1000 * 60 * 60 * 22))),
      44,
      true,
      None)
    Data.users.insert(user2)
    assert(user.get.email === "poweruser@poweruser.ch")

    user2.wishlists.associate(wishlist)

    val uwa2 = Data.getUserWishlistApp(2, "jamesclonk")
    assert(uwa2.isEmpty === true)
    assert(uwa2.size === 0)

    updateUserWishlists(user2, List("jamesclonk"))
    assert(wishlists.size === 1)

    val uwa3 = Data.getUserWishlistApp(2, "jamesclonk")
    assert(uwa3.isEmpty === false)
    assert(uwa3.size === wishlist.apps.size)

    assert(uwa3.forall(u => wishlist.apps.exists(_.appId == u.appId)), "all UWA apps exists in wishlist.apps link table")
    assert(wishlist.apps.forall(a => uwa3.exists(_.appId == a.id)), "all wishlist.apps exist in UWA table")
  }

  private def wishlists = from(Data.wishlists)(select(_)).toList
  private def uwa = from(Data.usersWishlistsApps)(select(_)).toList

  private def setupDatabase(): Unit = {
    Data.drop
    Data.create

    val user = User(1,
      "myuser@myuser.ch",
      "adbfadebf239930220",
      "a00a0a0beecc4432",
      new Timestamp(System.currentTimeMillis - (1 * (1000 * 60 * 60 * 22))),
      55,
      true,
      None)
    Data.users.insert(user)

    val wishlist = Wishlist("jamesclonk",
      new Timestamp(System.currentTimeMillis),
      new Timestamp(System.currentTimeMillis))
    Data.wishlists.insert(wishlist)

    user.wishlists.associate(wishlist)
  }
}
