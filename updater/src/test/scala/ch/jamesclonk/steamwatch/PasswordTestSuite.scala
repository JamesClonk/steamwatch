package ch.jamesclonk.steamwatch

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PasswordTestSuite
    extends FunSuite
    with Password {

  test("Password: Encryption, Salt & Checking") {
    val password1 = "Howdy!"
    generatePasswordAndSalt(password1)
    generatePasswordAndSalt(password1)
    val (encryptedPassword1, salt1) = generatePasswordAndSalt(password1)
    assert(encryptedPassword1 != password1)
    assert(salt1 != password1)
    assert(salt1.length === 18)
    assert(checkPassword(password1, salt1, encryptedPassword1) === true)
    assert(checkPassword(password1, salt1, "something") === false)

    val password2 = "Blablub!!!"
    val salt2 = "Muahaha!"
    val encryptedPassword2 = encryptPassword(password2, salt2)
    assert(encryptedPassword2 != password2)
    assert(encryptedPassword2 != encryptedPassword1)
    assert(checkPassword(password2, salt2, encryptedPassword2) === true)
    assert(checkPassword(password2, salt2, encryptedPassword1) === false)
  }

}
