package mau.utils

object Sanitizer:
  def sanitize(name: String): Option[String] =
    Option(name).map(_.strip).filter(_.nonEmpty)

  def isValid(name: String): Boolean =
    sanitize(name).contains(name)
