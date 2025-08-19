package mau.edition

import io.circe.Codec

case class CompilationReport(code: String, errors: Seq[CompilationError]) derives Codec:
  def isEmpty: Boolean = errors.isEmpty
