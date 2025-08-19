package mau.game

import io.circe.Codec
import mau.edition.CompilationReport
import mau.round.*

final case class StateProjection(you: Player, history: Seq[EventProjection], view: StateProjection.View) derives Codec

object StateProjection:
  enum View derives Codec:
    case Lobby(
      admin: Option[Player],
      statuses: Seq[(Player, StatusProjection)],
      customRulesNumber: Option[Int],
      latestWinner: Option[Player],
      latestRoundPlayers: Seq[Player]
    )
    case Round(
      buttons: Seq[Button],
      hand: Option[Hand],
      discard: Seq[Card],
      otherPlayers: Seq[(Player, Int)],
      effect: Option[EffectProjection],
      mustAct: MustActProjection
    )
    case Deliberation(
      suggestions: Seq[RuleSuggestion],
      initCompilation: CompilationReport
    )
