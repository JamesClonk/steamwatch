package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import org.squeryl.PrimitiveTypeMode._
import ch.jamesclonk.steamwatch.model.{ Data, User, Wishlist }
import ch.jamesclonk.steamwatch.Password
import java.sql.Timestamp

object CreateAccount
  extends Controller
  with Password {

  val createForm = Form(
    tuple(
      "email" -> text,
      "password" -> text,
      "code" -> text) verifying ("No! Something fishy is going on here..", result => result match {
        case (email, password, code) => check(email, password, code)
      }))

  def check(email: String, password: String, code: String): Boolean = {
    transaction {
      Data.getUserByEmail(email) match {
        case None if (code == "herebedragons! ;-)") => true
        case _ => false
      }
    }
  }

  def index = Action { implicit request =>
    Ok(views.html.create(createForm))
  }

  def create = Action { implicit request =>
    createForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.create(formWithErrors)),
      data => {
        // check again to be sure!
        if (check(data._1, data._2, data._3)) {
          inTransaction {
            val (pw, salt) = generatePasswordAndSalt(data._2)
            val user = User(0,
              data._1,
              pw,
              salt,
              new Timestamp(System.currentTimeMillis),
              55,
              true,
              None)
            Data.users.insert(user)
          }
          Ok(views.html.created(data))

        } else {
          Redirect(routes.CreateAccount.index).withNewSession.flashing(
            "error" -> "Error! Account was not created!")
        }
      })
  }
}