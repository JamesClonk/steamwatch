package controllers

import play.api._
import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import org.squeryl.PrimitiveTypeMode._
import ch.jamesclonk.steamwatch.model.{ Data, User }

trait Secured {

  def email(request: RequestHeader): Option[String] = request.session.get("email")

  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Auth.login)

  def withAuth(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(email, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }

  def withAuthUser(f: User => Request[AnyContent] => Result) = withAuth { email =>
    implicit request =>
      inTransaction {
        Data.getUserByEmail(email).map { user =>
          f(user)(request)
        }.getOrElse(onUnauthorized(request))
      }
  }

}