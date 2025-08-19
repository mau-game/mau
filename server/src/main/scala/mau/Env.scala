package mau

import scala.util.Properties

object Env:
  def envOrThrow(name: String): String =
    Properties.envOrElse(name, throw Exception(s"Missing env $name"))