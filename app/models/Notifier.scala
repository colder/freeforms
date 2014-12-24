package models

import db.Tables._

import play.api._

import javax.mail._
import javax.mail.internet._

import play.api.Logger
import play.api.Play.current

abstract class Notifier(ctx: Context) {
  def notify(n: Notification): Boolean
}

abstract class Notification {
  def to: FormsUsersRow

  def subject: String

  def content: String
}


case class FixedNotification(to: FormsUsersRow, subject: String, content: String) extends Notification

case class TemplateNotification(fh: FormHeader, to: FormsUsersRow, subjectTpl: String, contentTpl: String) extends Notification {
  import java.util.regex._;

  val pattern = Pattern.compile("\\[\\[(\\w+?)\\]\\]")

  val urlPrefix = {
    val url = current.configuration.getString("application.url").getOrElse("")
    if (url.endsWith("/")) {
      url.dropRight(1)
    } else {
      url
    }
  }

  def getTag(tag: String): String = tag match {
    case "to_firstname" =>
      to.firstname

    case "to_lastname" =>
      to.lastname

    case "to_fullname" =>
      to.firstname+" "+to.lastname

    case "student_fullname" =>
      fh.student.firstname+" "+fh.student.lastname

    case "form_title" =>
      fh.title

    case "form_fulltitle" =>
      "[[form_title]] - [[student_fullname]]"

    case "form_url" =>
      urlPrefix+controllers.routes.Forms.displayAuto(fh.faculty.name, fh.form.id, fh.student.sciper).toString

    case _ => "?"
  }

  def substitute(str: String): String = {
    var result = str

    val matcher = pattern.matcher(result)

    while(matcher.find()) {
      val res = matcher.toMatchResult
      val rep = getTag(res.group(1))
      result = result.substring(0, res.start) + rep + result.substring(res.end)
      matcher.reset(result)
    }

    result
  }

  def subject = substitute(subjectTpl)
  def content = substitute(contentTpl)

}


class EmailNotifier(ctx: Context) extends Notifier(ctx) {

  val cfg = current.configuration;

  val (props, auth) = {
    val ps = new java.util.Properties()

    ps.put("mail.smtp.host", cfg.getString("mail.smtp.host").getOrElse("localhost"))
    ps.put("mail.smtp.port", cfg.getInt("mail.smtp.port").getOrElse(25).toString)

    if (cfg.getBoolean("mail.smtp.ssl").getOrElse(false)) {
      //ps.put("mail.smtp.port", "465");
      ps.put("mail.smtp.ssl.enable", "true");
    }

    if (cfg.getBoolean("mail.smtp.auth").getOrElse(false)) {
      ps.put("mail.smtp.auth", "true")

      val username = cfg.getString("mail.smtp.username").get
      val password = cfg.getString("mail.smtp.password").get

      val auth = new Authenticator() {
        override def getPasswordAuthentication: PasswordAuthentication = {
          return new PasswordAuthentication(username, password);
        }
      };

      (ps, auth)
    } else {
      (ps, null)
    }
  }

  val sess = Session.getDefaultInstance(props, auth)

  def notify(n: Notification): Boolean = {
    val msg = new MimeMessage(sess)
    val fac = ctx.faculty

    try {
      if(n.to.email.nonEmpty) {
        msg.setFrom(new InternetAddress(fac.fromEmail, fac.fromName))
        msg.addRecipient(Message.RecipientType.TO,
          if (ctx.isDebug) {
            new InternetAddress("ekneuss@gmail.com")
          } else {
            new InternetAddress(n.to.email)
          }
        );

        (fac.replytoEmail, fac.replytoName) match {
          case (Some(email), Some(name)) =>
            msg.setReplyTo(Array(new InternetAddress(email, name)))

          case (Some(email), _) =>
            msg.setReplyTo(Array(new InternetAddress(email)))

          case _ =>
        }


        msg.setSubject(n.subject)
        msg.setText(n.content)

        Transport.send(msg)
        val to = if(ctx.isDebug) {
          n.to.email+"(->ekneuss@gmail.com)"
        } else {
          n.to.email
        }
        Logger.info("Sent notification to "+to+": "+n.subject)
        true
      } else {
        Logger.warn("User "+n.to.sciper+" has no registered e-mail.")
        false
      }
    } catch {
      case e: MessagingException =>
        Logger.warn("Failed to send notification to "+n.to.email+": "+n.subject, e)
        false
    }
  }
}
