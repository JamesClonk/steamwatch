package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import org.squeryl.PrimitiveTypeMode._
import ch.jamesclonk.steamwatch.model.{ Data, User }
import ch.jamesclonk.steamwatch.Password

object Auth
  extends Controller
  with Password {

  val loginForm = Form(
    tuple(
      "email" -> text,
      "password" -> text) verifying ("Invalid email or password", result => result match {
        case (email, password) => check(email, password)
      }))

  def check(email: String, password: String): Boolean = {
    inTransaction {
      Data.getUserByEmail(email) match {
        case None => false
        case Some(user) => checkPassword(password, user.salt, user.password)
      }
    }
  }

  def login = Action { implicit request =>
    Ok(views.html.login(loginForm))
  }

  def authenticate = Action { implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.login(formWithErrors)),
      user => Redirect(routes.Application.index).withSession("email" -> user._1))
  }

  def logout = Action {
    Redirect(routes.Auth.login).withNewSession.flashing(
      "success" -> "You are now logged out.")
  }
}