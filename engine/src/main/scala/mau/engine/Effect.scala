package mau.engine

import mau.round.*

enum Effect:
  case Penalised(player: Player, card: Card, action: Option[Action], effect: Option[Effect], reason: Reason)
  case CardPlayed(player: Player, card: Card)
  case CardDrawn(player: Player, card: Card)
  case ButtonClicked(player: Player, button: Button)

  def project(target: Player): EffectProjection =
    this match
      case Penalised(player, card, action, effect, reason) =>
        EffectProjection.Penalised(player, Option.when(player == target)(card), action, effect.map(_.project(target)), reason)
      case CardPlayed(player, card) => EffectProjection.CardPlayed(player, card)
      case CardDrawn(player, card) => EffectProjection.CardDrawn(player, Option.when(player == target)(card))
      case ButtonClicked(player, button) => EffectProjection.ButtonClicked(player, button)
