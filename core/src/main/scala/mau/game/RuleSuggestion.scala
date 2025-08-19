package mau.game

import io.circe.Codec
import mau.edition.CompilationReport

final case class RuleSuggestion(id: String, description: String, compilation: CompilationReport) derives Codec
