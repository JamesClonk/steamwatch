package ch.jamesclonk.steamwatch

import org.jasypt.util.password.StrongPasswordEncryptor
import org.jasypt.salt.RandomSaltGenerator

trait Password {

  val passwordEncryptor = new StrongPasswordEncryptor
  val saltGenerator = new RandomSaltGenerator

  def generatePasswordAndSalt(inputPassword: String): (String, String) = {
    val salt = new String(saltGenerator.generateSalt(18))
    val encryptedPassword = encryptPassword(inputPassword, salt)
    (encryptedPassword, salt)
  }
  def encryptPassword(inputPassword: String, salt: String): String =
    passwordEncryptor.encryptPassword(inputPassword + salt)
  def checkPassword(inputPassword: String, salt: String, encryptedPassword: String): Boolean =
    try {
      passwordEncryptor.checkPassword(inputPassword + salt, encryptedPassword)
    } catch {
      case _ => false
    }

}