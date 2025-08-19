package mau.edition

import io.circe.Codec

final case class CompilationParams(code: String) derives Codec
