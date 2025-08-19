package mau.round

import io.circe.Codec

enum EffectProjection derives Codec:
  case Penalised(player: Player, card: Option[Card], action: Option[Action], effect: Option[EffectProjection], reason: Reason)
  case CardPlayed(player: Player, card: Card)
  case CardDrawn(player: Player, card: Option[Card])
  case ButtonClicked(player: Player, button: Button)
