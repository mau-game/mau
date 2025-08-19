package mau.edition

import io.circe.Codec

final case class GenerationParams(prompt: String) derives Codec
