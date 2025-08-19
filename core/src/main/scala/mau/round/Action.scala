package mau.round

import io.circe.Codec
import scala.quoted.*

enum Action derives Codec:
  case Draw
  case Play(card: Card)
  case ClickButton(button: Button)
// case Give(player: Player, card: Card, receiver: Player)
// case Take(player: Player, target: Player)
// case Say(player: Player, msg: String)

object Action:
  object FIXME:
    inline def unapply(action: Action): Option[(Card)] = ${impl}
    private def impl(using Quotes): Expr[Option[(Card)]] =
      quotes.reflect.report.errorAndAbort(
        """|Replace this placeholder with a user action that triggers the rule:
           |  Action.Play(card)          // playing this card
           |  Action.Draw                // drawing a card
           |  Action.ClickButton(button) // click this button
           |""".stripMargin
      )
