package mau.game

import io.circe.Codec
import mau.round.Action

enum Command derives Codec:
  case ResetRules
  case StartPlaying
  case RoundAction(action: Action)
  case RefreshSuggestions
  case SelectRule(ruleId: String)
