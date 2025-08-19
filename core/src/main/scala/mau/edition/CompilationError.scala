package mau.edition

import io.circe.Codec

enum CompilationError derives Codec:
  case Fatal(className: String, message: Option[String], stackTrace: Seq[String])
  case Diagnostic(start: Int, end: Int, message: String)

object CompilationError:
  def fatal(cause: Throwable): CompilationError =
    Fatal(cause.getClass.getName, Option(cause.getMessage), cause.getStackTrace.map(_.toString))
