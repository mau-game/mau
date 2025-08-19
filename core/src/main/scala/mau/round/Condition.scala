package mau.round

import scala.quoted.*

object Condition:
  inline def FIXME : Boolean = ${impl}
  private def impl(using Quotes): Expr[Boolean] =
    quotes.reflect.report.errorAndAbort(
      """|Replace this placeholder with a condition such as:
         |  card.isDiamond                  // the card is a diamond
         |  card.isFace                     // the card is a face
         |  card.rank == Rank.Nine          // the card is a nine
         |  card.suit == state.discard.suit // the card has the same suit as the top card of the discard pile
         |  state.isExpectedPlayer          // you are not the expected player
         |  state.yourHandSize == 2         // you have 2 cards in your hand
         |""".stripMargin
    )