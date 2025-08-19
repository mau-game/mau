package mau.round

import io.circe.Codec
import scala.quoted.*

enum Decision derives Codec:
  case ValidCard, InvalidCard
  case ValidButton, InvalidButton
  case OutOfTurn, ValidTurn
  case TurnClockWise, TurnCounterClockWise, ReverseDirection
  case WinRound
  case MustClick(button: Button)

object Decision:
  inline def FIXME: Decision = ${impl}
  private def impl(using Quotes): Expr[Decision] =
    quotes.reflect.report.errorAndAbort(
      """|Replace this placeholder with a decision:
         |  Decision.ValidCard   // the player can play this card
         |  Decision.InvalidCard // the player cannot play this card
         |  Decision.OutOfTurn   // the player cannot play out-of-turn
         |  Decision.ValidTurn   // the player can play even if out-of-turn
         |  Decision.ValidButton   // the player clicked a valid button
         |  Decision.InvalidButton // the player clicked an invalid button
         |  Decision.TurnClockWise        // the game continues going clockwise
         |  Decision.TurnCounterClockWise // the game continues going counter-clockwise
         |  Decision.ReverDirection       // the game continues in reverse direction
         |  Decision.MustClick("<BUTTON NAME>") // the player must click a button
         |""".stripMargin
    )
  object MustClick:
    private def apply(button: Button): MustClick = new MustClick(button)
    def apply(button: String): MustClick = new MustClick(Button(button))
