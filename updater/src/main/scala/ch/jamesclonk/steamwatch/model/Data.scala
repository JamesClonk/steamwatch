package ch.jamesclonk.steamwatch.model

import scala.reflect.BeanProperty
import java.sql.Timestamp
import org.squeryl.KeyedEntity
import org.squeryl.PrimitiveTypeMode._
import org.squeryl.Schema
import org.squeryl.annotations.Column
import org.squeryl.dsl.ManyToOne
import org.squeryl.dsl.OneToMany
import org.squeryl.dsl.CompositeKey2
import org.squeryl.ForeignKeyDeclaration
import org.slf4j.{ Logger, LoggerFactory }
import org.squeryl.dsl.CompositeKey3

case class User(
    @BeanProperty @Column("ID") val id: Int,
    @BeanProperty @Column("EMAIL") val email: String,
    @BeanProperty @Column("PASSWORD") var password: String,
    @BeanProperty @Column("SALT") val salt: String,
    @BeanProperty @Column("CREATED_DATE") val created: Timestamp,
    @BeanProperty @Column("DEFAULT_PERCENTAGE") var defaultPercentage: Double,
    @BeanProperty @Column("VALIDATED") var validated: Boolean,
    @BeanProperty @Column("LAST_LOGIN_DATE") var lastLogin: Option[Timestamp]) extends KeyedEntity[Int] {
  lazy val wishlists = Data.linkUsersToWishlists.left(this)
  lazy val wishlistsAndApps = Data.linkUserWishlistAppsToUser.left(this)
  lazy val apps = this.wishlists.flatMap(w => w.apps).toList.distinct
  def delete() = {
    // had to do this because (older versions of) mysql does not support foreign key relations
    Data.usersWishlistsApps.deleteWhere(_.userId === this.id)
    Data.linkUsersToWishlists.deleteWhere(_.userId === this.id)
    Data.users.deleteWhere(_.id === this.id)
  }
  def upsert(): Unit = {
    val userFound = from(Data.users)(u => where(u.id === this.id) select (u))
    if (userFound.isEmpty) {
      Data.users.insert(this)
    } else {
      update(Data.users)(u => where(u.id === this.id)
        set (
          u.password := this.password,
          u.defaultPercentage := this.defaultPercentage,
          u.validated := this.validated,
          u.lastLogin := this.lastLogin))
    }
  }
}

case class App(
    @BeanProperty @Column("APP_ID") val id: Int,
    @BeanProperty @Column("NAME") var name: String,
    @BeanProperty @Column("LOGO") var logo: String,
    @BeanProperty @Column("STORE_LINK") var storeLink: String,
    @BeanProperty @Column("CREATED_DATE") val created: Timestamp,
    @BeanProperty @Column("CREATED_PRICE") val createdPrice: Double,
    @BeanProperty @Column("CURRENT_PRICE") var currentPrice: Double,
    @BeanProperty @Column("LAST_UPD_DATE") var lastUpdate: Timestamp) extends KeyedEntity[Int] {
  def appId = id
  lazy val usersAndWishlists = Data.linkUserWishlistAppsToApp.left(this)
  lazy val wishlists = Data.linkAppsToWishlists.left(this)
  lazy val users = this.wishlists.flatMap(w => w.users).toList.distinct
  def delete() = {
    // had to do this because (older versions of) mysql does not support foreign key relations
    Data.usersWishlistsApps.deleteWhere(_.appId === this.id)
    Data.linkAppsToWishlists.deleteWhere(_.appId === this.id)
    Data.apps.deleteWhere(_.id === this.id)
  }
  def upsert(): Unit = {
    val appFound = from(Data.apps)(a => where(a.id === this.id) select (a))
    if (appFound.isEmpty) {
      Data.apps.insert(this)
    } else {
      update(Data.apps)(a => where(a.id === this.id)
        set (
          a.name := this.name,
          a.logo := this.logo,
          a.storeLink := this.storeLink,
          a.currentPrice := this.currentPrice,
          a.lastUpdate := this.lastUpdate))
    }
  }
}

case class Wishlist(
    @BeanProperty @Column("STEAM_ID") val id: String,
    @BeanProperty @Column("CREATED_DATE") val created: Timestamp,
    @BeanProperty @Column("LAST_UPD_DATE") var lastUpdate: Timestamp) extends KeyedEntity[String] {
  lazy val usersAndApps = Data.linkUserWishlistAppsToWishlist.left(this)
  lazy val users = Data.linkUsersToWishlists.right(this)
  lazy val apps = Data.linkAppsToWishlists.right(this)
  def delete() = {
    // had to do this because (older versions of) mysql does not support foreign key relations
    Data.usersWishlistsApps.deleteWhere(_.wishlistId === this.id)
    Data.linkAppsToWishlists.deleteWhere(_.wishlistId === this.id)
    Data.linkUsersToWishlists.deleteWhere(_.wishlistId === this.id)
    Data.wishlists.deleteWhere(_.id === this.id)
  }
  def upsert(): Unit = {
    val wishlistFound = from(Data.wishlists)(w => where(w.id === this.id) select (w))
    if (wishlistFound.isEmpty) {
      Data.wishlists.insert(this)
    } else {
      update(Data.wishlists)(w => where(w.id === this.id)
        set (w.lastUpdate := this.lastUpdate))
    }
  }
}

case class UserWishlistApp(
    @BeanProperty @Column("USER_ID") val userId: Int,
    @BeanProperty @Column("WISHLIST_ID") val wishlistId: String,
    @BeanProperty @Column("APP_ID") val appId: Int,
    @BeanProperty @Column("ALERT_PERCENTAGE") var alertPercentage: Double,
    @BeanProperty @Column("ALERT_FLAG") var alert: Boolean,
    @BeanProperty @Column("ALERT_DATE") var alertDate: Option[Timestamp]) extends KeyedEntity[CompositeKey3[Int, String, Int]] {
  def id = compositeKey(userId, wishlistId, appId)
  lazy val user = Data.linkUserWishlistAppsToUser.right(this)
  lazy val wishlist = Data.linkUserWishlistAppsToWishlist.right(this)
  lazy val app = Data.linkUserWishlistAppsToApp.right(this)
  def delete() = Data.usersWishlistsApps.deleteWhere(_.id === this.id)
  def upsert(): Unit = {
    val userFound = from(Data.usersWishlistsApps)(uwa => where(uwa.id === this.id) select (uwa))
    if (userFound.isEmpty) {
      Data.usersWishlistsApps.insert(this)
    } else {
      update(Data.usersWishlistsApps)(uwa => where(uwa.id === this.id)
        set (uwa.alert := this.alert,
          uwa.alertPercentage := this.alertPercentage,
          uwa.alertDate := this.alertDate))
    }
  }
}

case class LinkUserWishlist(
    @BeanProperty @Column("USER_ID") val userId: Int,
    @BeanProperty @Column("WISHLIST_ID") val wishlistId: String) extends KeyedEntity[CompositeKey2[Int, String]] {
  def id = compositeKey(userId, wishlistId)
}

case class LinkAppWishlist(
    @BeanProperty @Column("APP_ID") val appId: Int,
    @BeanProperty @Column("WISHLIST_ID") val wishlistId: String) extends KeyedEntity[CompositeKey2[Int, String]] {
  def id = compositeKey(appId, wishlistId)
}

object Data extends Schema {
  private val logger = LoggerFactory.getLogger(this.getClass)

  val users = table[User]("USERS")
  val apps = table[App]("APPS")
  val wishlists = table[Wishlist]("WISHLISTS")
  val usersWishlistsApps = table[UserWishlistApp]("USERS_WISHLISTS_APPS")

  on(users)(u => declare(
    u.id is (autoIncremented("USER_ID_SEQUENCE"), unique, indexed("IDX_USERS_ID"), primaryKey),
    u.email is (unique, indexed("IDX_USERS_EMAIL")),
    u.password is (dbType("varchar(100)")),
    u.salt is (dbType("varchar(20)")),
    u.defaultPercentage defaultsTo (55.0d),
    u.validated defaultsTo (false)))

  on(apps)(g => declare(
    g.id is (unique, indexed("IDX_APPS_ID"), primaryKey),
    g.name is (unique, indexed("IDX_APPS_NAME"))))

  on(wishlists)(w => declare(
    w.id is (unique, indexed("IDX_WISHLISTS_ID"), primaryKey)))

  on(usersWishlistsApps)(uwa => declare(
    uwa.id is (unique, indexed("IDX_USERS_WISHLISTS_APPS_ID"), primaryKey),
    uwa.alertPercentage defaultsTo (55.0d),
    uwa.alert defaultsTo (true)))

  val linkUserWishlistAppsToUser =
    oneToManyRelation(users, usersWishlistsApps).
      via((u, l) => (l.userId === u.id))

  val linkUserWishlistAppsToWishlist =
    oneToManyRelation(wishlists, usersWishlistsApps).
      via((w, l) => (l.wishlistId === w.id))

  val linkUserWishlistAppsToApp =
    oneToManyRelation(apps, usersWishlistsApps).
      via((a, l) => (l.appId === a.id))

  val linkUsersToWishlists =
    manyToManyRelation(users, wishlists, "LINK_USERS_WISHLISTS").
      via[LinkUserWishlist]((u, w, l) => (l.wishlistId === w.id, u.id === l.userId))

  val linkAppsToWishlists =
    manyToManyRelation(apps, wishlists, "LINK_APPS_WISHLISTS").
      via[LinkAppWishlist]((a, w, l) => (l.wishlistId === w.id, a.id === l.appId))

  //  override def applyDefaultForeignKeyPolicy(foreignKeyDeclaration: ForeignKeyDeclaration) =
  //    foreignKeyDeclaration.constrainReference

  linkUserWishlistAppsToUser.foreignKeyDeclaration.constrainReference(onDelete cascade)
  linkUserWishlistAppsToWishlist.foreignKeyDeclaration.constrainReference(onDelete cascade)
  linkUserWishlistAppsToApp.foreignKeyDeclaration.constrainReference(onDelete cascade)

  linkUsersToWishlists.leftForeignKeyDeclaration.constrainReference(onDelete cascade)
  linkUsersToWishlists.rightForeignKeyDeclaration.constrainReference(onDelete cascade)
  linkAppsToWishlists.leftForeignKeyDeclaration.constrainReference(onDelete cascade)
  linkAppsToWishlists.rightForeignKeyDeclaration.constrainReference(onDelete cascade)

  def getUser(id: Int): Option[User] =
    try {
      Some(from(Data.users)(u => where(u.id === id) select (u)).single)
    } catch {
      case _ => None
    }
  def getUserByEmail(email: String): Option[User] =
    try {
      Some(from(Data.users)(u => where(u.email === email) select (u)).single)
    } catch {
      case _ => None
    }
  def getUsersByValidationStatus(status: Boolean): List[User] =
    from(Data.users)(u => where(u.validated === status) select (u)).toList
  def updateUserPassword(id: Int, password: String) =
    update(Data.users)(u => where(u.id === id) set (u.password := password))
  def updateUserDefaultPercentage(id: Int, percentage: Double) =
    update(Data.users)(u => where(u.id === id) set (u.defaultPercentage := percentage))
  def updateUserValidationStatus(id: Int, status: Boolean) =
    update(Data.users)(u => where(u.id === id) set (u.validated := status))
  def updateUserLastLogin(id: Int, login: Timestamp) =
    update(Data.users)(u => where(u.id === id) set (u.lastLogin := Some(login)))
  def getExpiredUnvalidatedUsers(): List[User] =
    from(Data.users)(u =>
      where((u.validated === false) and
        (u.created < new Timestamp(System.currentTimeMillis - (1000 * 60 * 60 * 24)))) select (u)).toList
  def deleteExpiredUnvalidatedUsers(): Unit = {
    val expired = getExpiredUnvalidatedUsers()
    expired foreach (_.wishlistsAndApps foreach (uwa => uwa.delete()))
    expired foreach (_.delete())
  }

  def getWishlist(steamId: String): Option[Wishlist] =
    try {
      Some(from(Data.wishlists)(w => where(w.id === steamId) select (w)).single)
    } catch {
      case _ => None
    }
  def getUnusedWishlists(): List[Wishlist] =
    Data.wishlists filter (_.users.isEmpty) toList
  def deleteUnusedWishlists(): Unit = {
    val unused = getUnusedWishlists()
    unused foreach (_.usersAndApps foreach (_.delete()))
    unused foreach (_.delete())
  }
  def getEmptyWishlists(): List[Wishlist] =
    Data.wishlists filter (_.apps.isEmpty) toList
  def deleteEmptyWishlists(): Unit = {
    val empty = getEmptyWishlists()
    empty foreach (_.usersAndApps foreach (_.delete()))
    empty foreach (_.delete())
  }

  def getApp(id: Int): Option[App] =
    try {
      Some(from(Data.apps)(a => where(a.id === id) select (a)).single)
    } catch {
      case _ => None
    }

  def getUserWishlistApp(userId: Int, wishlistId: String): List[UserWishlistApp] =
    from(Data.usersWishlistsApps)(uwa =>
      where(uwa.userId === userId
        and uwa.wishlistId === wishlistId) select (uwa)).toList
  def getUserWishlistApp(userId: Int, wishlistId: String, appId: Int): Option[UserWishlistApp] =
    try {
      Some(from(Data.usersWishlistsApps)(uwa =>
        where(uwa.userId === userId
          and uwa.wishlistId === wishlistId
          and uwa.appId === appId) select (uwa)).single)
    } catch {
      case _ => None
    }
  def getNotifiableUsersWishlistsApps(): List[UserWishlistApp] =
    from(Data.apps, Data.usersWishlistsApps)((a, uwa) =>
      where(uwa.appId === a.id
        and (uwa.alert === true)
        and (a.currentPrice.toDouble lte (a.createdPrice.toDouble times (uwa.alertPercentage.toDouble div 100.0d))))
        select (uwa)).toList
  def getOrphanUsersWishlistsApps(): List[UserWishlistApp] = {
    val apps = Data.usersWishlistsApps filter (_.app.isEmpty) toList
    val users = Data.usersWishlistsApps filter (_.user.isEmpty) toList
    val wishlists = Data.usersWishlistsApps filter (_.wishlist.isEmpty) toList
    val result = apps.union(users).union(wishlists)
    result.distinct
  }
  def deleteOrphanUsersWishlistsApps(): Unit =
    getOrphanUsersWishlistsApps() foreach (_.delete())
  def getUnusedUsersWishlistsApps(): List[UserWishlistApp] =
    Data.usersWishlistsApps.filterNot(uwa => uwa.wishlist.single.users.map(_.id).toList.contains(uwa.userId)).toList
  def deleteUnusedUsersWishlistsApps(): Unit =
    getUnusedUsersWishlistsApps() foreach (_.delete())
  def deleteUserWishlistApp(wishlistId: String): Unit =
    Data.usersWishlistsApps.deleteWhere(_.wishlistId === wishlistId)
  def deleteUserWishlistApp(wishlistId: String, appId: Int): Unit =
    Data.usersWishlistsApps.deleteWhere(uwa =>
      uwa.wishlistId === wishlistId
        and uwa.appId === appId)
}

