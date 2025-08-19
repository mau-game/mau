/* package mau

import munit._
import Values._
import mau.Rank._
import mau.Suit._

class RoundSimulation extends FunSuite:
  test("adpi2 wins") {
    val initialState = Values.twoPlayers()
    val actions = Seq(
      Action.Play(player1, Card(Three, Spade)),
      Action.Draw(player2),
      Action.Play(player1, Card(Ace, Spade)),
      Action.Play(player2, Card(Six, Spade)),
      Action.Draw(player1),
      Action.Draw(player2),
      Action.Draw(player1),
      Action.Play(player2, Card(Six, Heart)),
      Action.Play(player1, Card(Three, Heart)),
      Action.Play(player2, Card(Ten, Heart)),
      Action.Draw(player1),
      Action.Draw(player2),
      Action.Play(player1, Card(Jack, Heart)),
      Action.Draw(player2),
      Action.Draw(player1),
      Action.Draw(player2),
      Action.Draw(player1),
      Action.Play(player2, Card(Jack, Spade)),
      Action.Draw(player1),
      Action.Play(player2, Card(Eight, Spade)),
      Action.Play(player1, Card(Eight, Club)),
      Action.Play(player2, Card(Four, Club)),
      Action.Play(player1, Card(Ace, Club)),
      Action.Play(player2, Card(Nine, Club)),
      Action.Play(player1, Card(Five, Club)),
      Action.Play(player2, Card(Five, Diamond)),
      Action.Play(player1, Card(Ten, Diamond)),
      Action.Play(player2, Card(Four, Diamond)),
      Action.Play(player1, Card(Two, Diamond)),
      Action.Play(player2, Card(Two, Spade))
    )
    val state = actions.foldLeft(initialState) { (state, action) => RoundEngine.handle(state, Rule.baseRules, action) }
    assert(state.winner.isDefined)
  } */