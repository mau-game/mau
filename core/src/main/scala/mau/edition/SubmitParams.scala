package mau.edition

import io.circe.Codec

final case class SubmitParams(code: String) derives Codec
