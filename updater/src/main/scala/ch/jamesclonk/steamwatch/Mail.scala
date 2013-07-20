package ch.jamesclonk.steamwatch

import org.apache.commons.mail.HtmlEmail
import java.net.URL
import com.typesafe.config.Config
import ch.jamesclonk.steamwatch.model._

trait Mail {

  val config: Config

  private val header = """
<html><head><title>SteamWatch Sales Notification</title></head><body style="font-family: arial; font-size: 12px; color: #333333">
<table cellpadding="0" cellspacing="0" width="100%">
<tr><td style="font-size: 12px"><center>   
<table bgcolor="#efefef" cellpadding="10px" cellspacing="0" height="100%" style="color: #444444; padding: 15px 0px 15px 0px" width="100%">
"""
  private val footer = """
</table></center>
</td></tr></table></body></html>
"""
  private def beginList(wishlistId: String): String = """
	<tr><td align="center" valign="top">  
	<table border="0" cellpadding="0" cellspacing="0" style="border: 0 none transparent; border-collapse: collapse; border-spacing: 0px; padding: 0px" width="500px">
	<tr bgcolor="#ffffff" style="background-color: #ffffff">
	<td align="left" valign="middle">
	<h1 style="font-family: Helvetica, Arial, sans-serif; font-size: 22px; line-height: 28px; padding: 30px 30px; color: #555555; margin: 0px">
	Sales from wishlist [ <a href="http://steamcommunity.com/id/""" + wishlistId + """/wishlist" style="text-decoration: none; color: #55a0f0">""" + wishlistId + """</a> ]:</h1>
	</td></tr>
"""
  private val endList = """
	</table>
	</td></tr>
"""
  private def formatApp(cid: String, app: App): String = """
		<tr bgcolor="#ffffff" style="background-color: #ffffff">
		<td align="left" valign="middle">
		<table>
		<tr>
		<td align="left" valign="top">
		<a href="""" + app.storeLink + """">
		<img alt="""" + app.name + """" src="cid:""" + cid + """" style="border:2px solid #e6e6e6; margin: 0 0 0 30px" width="184px" />
		</a>
		</td>
		<td align="left" valign="middle">
		<h2 style="font-family: Helvetica, Arial, sans-serif; font-size: 14px; line-height: 20px; margin: 0px 40px 0px 24px; padding: 0px 0px 0px">
		<a href="""" + app.storeLink + """" style="text-decoration: none; color: #55a0f0">""" + app.name + """</a>
		</h2>
		<p style="font-family: Helvetica, Arial, sans-serif; font-size: 14px; line-height: 20px; margin: 0px 40px 0px 24px; padding: 0px 0px 0px">Current Price: """ + (app.currentPrice / 100.0d) + """&euro;</p>
		<p style="font-family: Helvetica, Arial, sans-serif; font-size: 11px; line-height: 20px; margin: 0px 40px 0px 24px; padding: 0px 0px 0px; text-decoration: line-through">Initial Price: """ + (app.createdPrice / 100.0d) + """&euro;</p>
		</td>
		</tr>
		</table>
		</td></tr>
"""

  private lazy val emailAdmin = config.getString("email.admin")
  private lazy val emailHost = config.getString("email.host")
  private lazy val emailPort = config.getInt("email.port")
  private lazy val emailSsl = config.getBoolean("email.ssl")
  private lazy val emailUsername = config.getString("email.username")
  private lazy val emailPassword = config.getString("email.password")
  private lazy val emailFrom = config.getString("email.from")
  private lazy val emailSubject = config.getString("email.subject")

  private def getEmail(to: String): HtmlEmail = {
    val email = new HtmlEmail
    email.setDebug(false)
    email.setHostName(emailHost)
    email.setSmtpPort(emailPort)
    email.setSSL(emailSsl)
    email.setAuthentication(emailUsername, emailPassword)
    email.addTo(to, to)
    email.setFrom(emailUsername, emailFrom)
    email.setSubject(emailSubject)
    email
  }

  def sendMail(to: String, sales: Map[Wishlist, List[App]]): Unit = {
    val email = getEmail(to)

    val message = new StringBuilder
    message.append(header)

    sales foreach { sale =>
      val (wishlist, apps) = sale
      message.append(beginList(wishlist.id))

      apps foreach { app =>
        val url = new URL(app.logo)
        val cid = email.embed(url, app.name)
        message.append(formatApp(cid, app))
      }

      message.append(endList)
    }
    message.append(footer)

    email.setHtmlMsg(message.toString)
    email.setTextMsg("Your email client does not support HTML messages")

    email.send()
  }

  def sendErrorMail(ex: Exception): Unit = {
    val email = getEmail(emailAdmin)

    val message = new StringBuilder
    message.append("<html><body><h1>An exception occured!</h1><br/>")
    message.append(ex.toString)
    message.append("<br/>")
    message.append(ex.getMessage)
    message.append("<br/>")
    message.append(ex.getStackTrace.mkString("<br/>"))
    message.append("</body></html>")

    email.setHtmlMsg(message.toString)
    email.setTextMsg("An exception occured!\n\n" + ex.getMessage)

    email.send()
  }

}