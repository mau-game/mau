package mau.edition

import io.circe.Codec

final case class CompletionParams(code: String, position: Int) derives Codec
