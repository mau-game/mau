package mau.edition

import io.circe.Codec

enum Generation derives Codec:
  case Success(compilation: CompilationReport, rephrasedRule: String)
  case Failure(error: String)
