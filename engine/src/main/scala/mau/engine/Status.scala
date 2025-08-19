package mau.engine

import mau.edition.CompilationReport
import mau.game.RuleSuggestion
import mau.game.StatusProjection

enum Status:
  case Deliberating(suggestions: Seq[RuleSuggestion])
  case Playing
  case InLobby

  def projection: StatusProjection = this match
    case Deliberating(_) => StatusProjection.Deliberating
    case Playing => StatusProjection.Playing
    case InLobby => StatusProjection.InLobby
