package ch.jamesclonk.steamwatch.model

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

@RunWith(classOf[JUnitRunner])
class DataTestSuite
    extends FunSuite
    with BeforeAndAfter {

  var session: Session = null

  before {
    Class.forName("org.h2.Driver")

    SessionFactory.concreteFactory = Some(() =>
      Session.create(DriverManager.getConnection("jdbc:h2:data/steamwatch_unittest_data"), new H2Adapter))

    session = SessionFactory.newSession
    session.bindToCurrentThread

    setupDatabase()
  }

  after {
    session.cleanup
    session.close
  }

  test("Users: getUser") {
    val user1 = Data.getUser(1).get
    assert(user1.id === 1)
    assert(user1.email === "myuser1@myuser.ch")
    assert(user1.password === "adbfadebf239930220")
    assert(user1.salt === "a00a0a0beecc4432")
    assert(user1.lastLogin === None)
    assert(user1.validated === false)

    val user4 = Data.getUser(4).get
    assert(user4.id === 4)
    assert(user4.email === "myuser4@myuser.ch")
    assert(user4.password === "adbfadebf239930220")
    assert(user4.salt === "a00a0a0beecc4432")
    assert(user4.lastLogin === None)
    assert(user4.validated === true)

    assert(user1 != user4)
  }

  test("Users: getUserByEmail") {
    val user1 = Data.getUserByEmail("myuser1@myuser.ch").get
    assert(user1.id === 1)
    assert(user1.email === "myuser1@myuser.ch")
    assert(user1.password === "adbfadebf239930220")
    assert(user1.salt === "a00a0a0beecc4432")
    assert(user1.lastLogin === None)
    assert(user1.validated === false)

    val user4 = Data.getUserByEmail("myuser4@myuser.ch").get
    assert(user4.id === 4)
    assert(user4.email === "myuser4@myuser.ch")
    assert(user4.password === "adbfadebf239930220")
    assert(user4.salt === "a00a0a0beecc4432")
    assert(user4.lastLogin === None)
    assert(user4.validated === true)

    assert(user1 != user4)
  }

  test("Users: getUsersByValidationStatus") {
    assert(Data.getUsersByValidationStatus(true).size === 2)
    assert(Data.getUsersByValidationStatus(false).size === 3)

    val users = Data.getUsersByValidationStatus(true)
    users.foreach { user =>
      assert(user.validated === true)
    }
  }

  test("Users: updateUserPassword") {
    assert(Data.getUser(1).get.password === "adbfadebf239930220")

    Data.updateUserPassword(1, "hello!")
    assert(Data.getUser(1).get.password === "hello!")

    Data.updateUserPassword(5, "abc")
    assert(Data.getUser(5).get.password === "abc")
  }

  test("Users: updateUserDefaultPercentage") {
    assert(Data.getUser(1).get.defaultPercentage === 55)

    Data.updateUserDefaultPercentage(1, 25)
    assert(Data.getUser(1).get.defaultPercentage === 25)

    Data.updateUserDefaultPercentage(1, 60)
    assert(Data.getUser(1).get.defaultPercentage === 60)
  }

  test("Users: updateUserValidationStatus") {
    assert(Data.getUser(1).get.validated === false)

    Data.updateUserValidationStatus(1, true)
    assert(Data.getUser(1).get.validated === true)

    Data.updateUserValidationStatus(1, false)
    assert(Data.getUser(1).get.validated === false)
  }

  test("Users: updateUserLastLogin") {
    val previousLogin = Data.getUser(1).get.lastLogin
    assert(previousLogin === None)
    assert(Data.getUser(1).get.lastLogin === None)

    val newLogin = new Timestamp(System.currentTimeMillis)
    Data.updateUserLastLogin(1, newLogin)
    assert(Data.getUser(1).get.lastLogin.get === newLogin)
  }

  test("Users: getExpiredUnvalidatedUsers") {
    val expired = Data.getExpiredUnvalidatedUsers()

    assert(users.size === 5)
    assert(users.exists(_.id == 3))
    assert(users.exists(_.id == 5))
  }

  test("Users: deleteExpiredUnvalidatedUsers") {
    assert(users.size === 5)

    Data.deleteExpiredUnvalidatedUsers()
    assert(users.size === 3)
    assert(users.forall(_.id != 3))
    assert(users.forall(_.id != 5))
  }

  test("Users: delete") {
    assert(users.size === 5)

    val user5 = Data.getUser(5).get
    user5.delete()
    assert(users.size === 4)
  }

  test("Users: upsert") {
    assert(users.size === 5)

    val user2 = Data.getUser(2)
    user2.get.upsert()
    assert(users.size === 5)
    assert(Data.getUser(2) === user2)

    val userNew = User(99,
      "99@99.com",
      "abc123",
      "999",
      new Timestamp(12345),
      55,
      true,
      Some(new Timestamp(999999)))
    userNew.upsert()
    assert(users.size === 6)
    assert(Data.getUser(6).get === userNew)
    assert(Data.getUser(6).get.email === "99@99.com")
    assert(Data.getUser(6).get.lastLogin.get === new Timestamp(999999))

    userNew.password = "666666"
    userNew.lastLogin = Some(new Timestamp(1000000))
    userNew.upsert()
    assert(Data.getUser(6).get.password === "666666")
    assert(Data.getUser(6).get.lastLogin === Some(new Timestamp(1000000)))

    val user5 = Data.getUser(5).get
    user5.password = "12345"
    user5.upsert()
    assert(users.size === 6)
    assert(Data.getUser(5).get === user5)
    assert(Data.getUser(5).get.password === "12345")
  }

  test("Wishlists: getWishlist") {
    val wishlist1 = Data.getWishlist("Wlu1").get
    assert(wishlist1.id === "Wlu1")
    assert(wishlist1.created != null)
    assert(wishlist1.lastUpdate != null)

    val wishlist2 = Data.getWishlist("Wlu2").get
    assert(wishlist2.id === "Wlu2")
    assert(wishlist2.created != null)
    assert(wishlist2.lastUpdate != null)

    assert(wishlist1 != wishlist2)
  }

  test("Wishlists: getUnusedWishlists") {
    val wishlists = Data.getUnusedWishlists()
    assert(wishlists.size === 3)
    assert(wishlists(0).id === "Wlu3")
    assert(wishlists(1).id === "Wlu4")
    assert(wishlists(2).id === "Wlu5")
  }

  test("Wishlists: deleteUnusedWishlists") {
    def unusedWishlists = Data.getUnusedWishlists()
    assert(wishlists.size === 5)
    assert(unusedWishlists.size === 3)

    Data.deleteUnusedWishlists()

    assert(wishlists.size === 2)
    assert(unusedWishlists.size === 0)
  }

  test("Wishlists: getEmptyWishlists") {
    val wishlists = Data.getEmptyWishlists()
    assert(wishlists.size === 1)
    assert(wishlists(0).id === "Wlu3")
  }

  test("Wishlists: deleteEmptyWishlists") {
    def emptyWishlists = Data.getEmptyWishlists()
    assert(wishlists.size === 5)
    assert(emptyWishlists.size === 1)

    Data.deleteEmptyWishlists()

    assert(wishlists.size === 4)
    assert(emptyWishlists.size === 0)
  }

  test("Wishlists: delete") {
    assert(wishlists.size === 5)

    val wishlist5 = Data.getWishlist("Wlu5").get
    wishlist5.delete()
    assert(wishlists.size === 4)
  }

  test("Wishlists: upsert") {

    assert(wishlists.size === 5)

    val wishlist1 = Data.getWishlist("Wlu1")
    wishlist1.get.upsert()
    assert(wishlists.size === 5)
    assert(Data.getWishlist("Wlu1") === wishlist1)

    val wishlistNew = Wishlist("Wlu77",
      new Timestamp(12345),
      new Timestamp(999999))
    wishlistNew.upsert()
    assert(wishlists.size === 6)
    assert(Data.getWishlist("Wlu77").get === wishlistNew)
    assert(Data.getWishlist("Wlu77").get.created === new Timestamp(12345))
    assert(Data.getWishlist("Wlu77").get.lastUpdate === new Timestamp(999999))

    wishlistNew.lastUpdate = new Timestamp(1000000)
    wishlistNew.upsert()
    assert(Data.getWishlist("Wlu77").get.created === new Timestamp(12345))
    assert(Data.getWishlist("Wlu77").get.lastUpdate === new Timestamp(1000000))

    val wishlist5 = Data.getWishlist("Wlu5")
    wishlist5.get.lastUpdate = new Timestamp(666)
    wishlist5.get.upsert()
    assert(wishlists.size === 6)
    assert(Data.getWishlist("Wlu5") === wishlist5)
    assert(Data.getWishlist("Wlu5").get.lastUpdate === new Timestamp(666))
  }

  test("Apps: getApp") {
    val app1 = Data.getApp(51001).get
    assert(app1.id === 51001)
    assert(app1.name === "Game 1")
    assert(app1.logo === "logoURL")
    assert(app1.storeLink === "storeURL")
    assert(app1.created != null)
    assert(app1.createdPrice === 100)
    assert(app1.currentPrice === 89)
    assert(app1.lastUpdate != null)

    val app5 = Data.getApp(51005).get
    assert(app5.id === 51005)
    assert(app5.name === "Game 5")
    assert(app5.logo === "logoURL")
    assert(app5.storeLink === "storeURL")
    assert(app5.created != null)
    assert(app5.createdPrice === 480)
    assert(app5.currentPrice === 429)
    assert(app5.lastUpdate != null)

    assert(app1 != app5)
  }

  test("Apps: delete") {
    assert(apps.size === 6)

    val app5 = Data.getApp(51005).get
    app5.delete()
    assert(apps.size === 5)
  }

  test("Apps: upsert") {
    assert(apps.size === 6)

    val app2 = Data.getApp(51002)
    app2.get.upsert()
    assert(apps.size === 6)
    assert(Data.getApp(51002) === app2)

    val appNew = App(51099,
      "Game 99",
      "logoURL",
      "storeURL",
      new Timestamp(System.currentTimeMillis),
      99,
      99,
      new Timestamp(System.currentTimeMillis))
    appNew.upsert()
    assert(apps.size === 7)
    assert(Data.getApp(51099).get === appNew)
    assert(Data.getApp(51099).get.name === "Game 99")

    appNew.name = "Game 666"
    appNew.logo = "Muaha"
    appNew.upsert()
    assert(Data.getApp(51099).get.name === "Game 666")
    assert(Data.getApp(51099).get.logo === "Muaha")

    val app5 = Data.getApp(51005).get
    app5.currentPrice = 12345
    app5.upsert()
    assert(apps.size === 7)
    assert(Data.getApp(51005).get === app5)
    assert(Data.getApp(51005).get.currentPrice === 12345)
  }

  test("UsersWishlistsApps: getUserWishlistApp - 2 params") {
    val uwas = Data.getUserWishlistApp(1, "Wlu3")
    assert(uwas.size === 2)
    assert(uwas(0).wishlistId === "Wlu3")
    assert(uwas(0).alertPercentage === 22)
  }

  test("UsersWishlistsApps: getUserWishlistApp - 3 params") {
    val uwa1 = Data.getUserWishlistApp(1, "Wlu1", 51000).get
    assert(uwa1.wishlistId === "Wlu1")
    assert(uwa1.alertPercentage === 11)

    val uwa2 = Data.getUserWishlistApp(1, "Wlu3", 51000).get
    assert(uwa2.wishlistId === "Wlu3")
    assert(uwa2.alertPercentage === 22)

    assert(uwa1 != uwa2)
  }

  test("UsersWishlistsApps: getNotifiableUsersWishlistsApps") {
    val uwas = Data.getNotifiableUsersWishlistsApps()
    assert(uwas.size === 1)
  }

  test("UsersWishlistsApps: getOrphanUsersWishlistsApps") {
    // this unit test is pointless for a "good" db like h2.
    // but the problem is that (old versions of) mysql does not adhere to onCascade-delete!
    val uwas = Data.getOrphanUsersWishlistsApps()
    assert(uwas.size === 0)
  }

  test("UsersWishlistsApps: deleteOrphanUsersWishlistsApps") {
    // this unit test is pointless for a "good" db like h2.
    // but the problem is that (old versions of) mysql does not adhere to onCascade-delete!
    def orphans = Data.getOrphanUsersWishlistsApps()
    assert(uwas.size === 3)
    assert(orphans.size === 0)

    Data.deleteOrphanUsersWishlistsApps()
    assert(uwas.size === 3)
    assert(orphans.size === 0)
  }

  test("UsersWishlistsApps: getUnusedUsersWishlistsApps") {
    val uwas = Data.getUnusedUsersWishlistsApps()
    assert(uwas.size === 2)
    assert(uwas(0).wishlistId === "Wlu3")
    assert(uwas(1).wishlistId === "Wlu3")
    assert(uwas(0).userId === 1)
    assert(uwas(1).userId === 1)
    assert(uwas(0).appId === 51000)
    assert(uwas(1).appId === 51001)
  }

  test("UsersWishlistsApps: deleteUnusedUsersWishlistsApps") {
    def unusedUwas = Data.getUnusedUsersWishlistsApps()
    assert(uwas.size === 3)
    assert(unusedUwas.size === 2)

    Data.deleteUnusedUsersWishlistsApps()

    assert(uwas.size === 1)
    assert(unusedUwas.size === 0)
  }

  test("UsersWishlistsApps: deleteUserWishlistApp(wishlistId)") {
    assert(uwas.size === 3)

    Data.deleteUserWishlistApp("Wlu3")
    assert(uwas.size === 1)
  }

  test("UsersWishlistsApps: deleteUserWishlistApp(wishlistId,appId)") {
    assert(uwas.size === 3)

    Data.deleteUserWishlistApp("Wlu3", 51001)
    assert(uwas.size === 2)
  }

  test("UsersWishlistsApps: delete") {
    assert(uwas.size === 3)

    val uwa2 = Data.getUserWishlistApp(1, "Wlu3", 51000).get
    uwa2.delete()
    assert(uwas.size === 2)
  }

  test("UsersWishlistsApps: upsert") {
    assert(uwas.size === 3)

    val uwa1 = Data.getUserWishlistApp(1, "Wlu1", 51000)
    uwa1.get.upsert()
    assert(uwas.size === 3)
    assert(Data.getUserWishlistApp(1, "Wlu1", 51000) === uwa1)

    val uwaNew = UserWishlistApp(1, "Wlu1", 51004, 666, true, None)
    uwaNew.upsert()
    assert(uwas.size === 4)
    assert(Data.getUserWishlistApp(1, "Wlu1", 51004).get === uwaNew)
    assert(Data.getUserWishlistApp(1, "Wlu1", 51004).get.alertPercentage === 666)

    uwaNew.alert = false
    uwaNew.alertPercentage = 777
    uwaNew.upsert()
    assert(uwas.size === 4)
    assert(Data.getUserWishlistApp(1, "Wlu1", 51004).get.alert === false)
    assert(Data.getUserWishlistApp(1, "Wlu1", 51004).get.alertPercentage === 777)

    val uwa3 = Data.getUserWishlistApp(1, "Wlu3", 51001).get
    uwa3.alertPercentage = 12345
    uwa3.upsert()
    assert(uwas.size === 4)
    assert(Data.getUserWishlistApp(1, "Wlu3", 51001).get === uwa3)
    assert(Data.getUserWishlistApp(1, "Wlu3", 51001).get.alertPercentage === 12345)
  }

  test("delete[onCascade]: does not 'accidentally' remove users, apps or wishlists") {
    assert(users.size === 5)
    assert(apps.size === 6)

    val user1 = Data.getUser(1).get
    assert(user1.apps.size === 3)
    assert(user1.wishlists.size === 1)

    val app1 = Data.getApp(51001).get
    assert(!app1.users.isEmpty)
    assert(!app1.users.toList.contains(Data.getUser(1).get))
    assert(!app1.users.toList.contains(Data.getUser(5).get))
    assert(app1.users.toList.contains(Data.getUser(2).get))
    assert(app1.wishlists.toList.contains(Data.getWishlist("Wlu4").get))

    val wishlist1 = Data.getWishlist("Wlu1").get
    assert(!wishlist1.users.isEmpty)
    assert(wishlist1.users.toList.contains(Data.getUser(1).get))
    assert(!wishlist1.users.toList.contains(Data.getUser(2).get))
    assert(wishlist1.apps.toList.contains(Data.getApp(51005).get))

    user1.delete()
    assert(users.size === 4)

    assert(!app1.users.isEmpty)
    assert(!app1.users.toList.contains(Data.getUser(1).getOrElse(null)))
    assert(app1.users.toList.contains(Data.getUser(2).get))
    assert(app1.wishlists.toList.contains(Data.getWishlist("Wlu4").get))

    assert(wishlist1.users.isEmpty)
    assert(wishlist1.apps.toList.contains(Data.getApp(51005).get))

    Data.deleteUnusedWishlists()
    assert(Data.getWishlist("Wlu1") === None)
    assert(apps.size === 6)
  }

  test("delete[onCascade]: removes entries from userWishlistApp when deleting a wishlists or user") {
    val wishlist2 = Data.getWishlist("Wlu3").get
    assert(wishlist2.users.isEmpty)

    val uwa2 = Data.getUserWishlistApp(1, "Wlu3", 51000).get
    assert(uwa2.alertPercentage === 22)

    wishlist2.delete()
    assert(Data.getUserWishlistApp(1, "Wlu3", 51000) === None)

    val wishlist1 = Data.getWishlist("Wlu1").get
    assert(!wishlist1.users.isEmpty)

    val uwa1 = Data.getUserWishlistApp(1, "Wlu1", 51000).get
    assert(uwa1.alertPercentage === 11)

    val user1 = Data.getUser(1).get
    assert(user1.wishlists.size === 1)

    user1.delete()
    assert(Data.getUserWishlistApp(1, "Wlu1", 51000) === None)
  }

  private def users = from(Data.users)(select(_)).toList
  private def apps = from(Data.apps)(select(_)).toList
  private def wishlists = from(Data.wishlists)(select(_)).toList
  private def uwas = from(Data.usersWishlistsApps)(select(_)).toList

  private def setupDatabase(): Unit = {
    //Data.printDdl
    Data.drop
    Data.create

    val newUsers = (1 to 5) map { e =>
      User(e,
        "myuser" + e + "@myuser.ch",
        "adbfadebf239930220",
        "a00a0a0beecc4432",
        new Timestamp(System.currentTimeMillis - (e * (1000 * 60 * 60 * 22))),
        55,
        e % 2 == 0,
        None)
    }
    Data.users.insert(newUsers)

    val newApps = (0 to 5) map { e =>
      App(e + 51000,
        "Game " + e,
        "logoURL",
        "storeURL",
        new Timestamp(System.currentTimeMillis + e),
        (e * 95) + 5,
        (e * 85) + 4,
        new Timestamp(System.currentTimeMillis + e))
    }
    Data.apps.insert(newApps)

    val newWishlists = (1 to 5) map { e =>
      Wishlist("Wlu" + e,
        new Timestamp(System.currentTimeMillis + e),
        new Timestamp(System.currentTimeMillis + e))
    }
    Data.wishlists.insert(newWishlists)

    newApps(1).wishlists.associate(newWishlists(1))
    newApps(3).wishlists.associate(newWishlists(0))
    newApps(4).wishlists.associate(newWishlists(0))
    newApps(5).wishlists.associate(newWishlists(0))
    newApps(1).wishlists.associate(newWishlists(3))
    newApps(2).wishlists.associate(newWishlists(3))
    newApps(3).wishlists.associate(newWishlists(3))
    newApps(4).wishlists.associate(newWishlists(4))

    Data.getUser(1).get.wishlists.associate(newWishlists(0))
    Data.getUser(2).get.wishlists.associate(newWishlists(1))

    val uwa1 = UserWishlistApp(1, "Wlu1", 51000, 11, true, None)
    Data.usersWishlistsApps.insert(uwa1)
    val uwa2 = UserWishlistApp(1, "Wlu3", 51000, 22, true, None)
    Data.usersWishlistsApps.insert(uwa2)
    val uwa3 = UserWishlistApp(1, "Wlu3", 51001, 90, true, None)
    Data.usersWishlistsApps.insert(uwa3)
  }
}

