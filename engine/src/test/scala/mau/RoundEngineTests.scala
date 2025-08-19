/* package mau
import munit._
import Values._
import mau.Rank._
import mau.Suit._

class RoundEngineTests extends FunSuite:
  test("base rules: play card of same suit"):
    val state0 = Values.threePlayers()
    val WithUntrustedRules(state, _) = RoundEngine.handle(state0, Rule.baseRules, Action.Play(player1, Card(Two, Diamond)))
    assertEquals(state.discard.head, Card(Two, Diamond))
    assertEquals(state.hands(player1).size, 4)
    assertEquals(state.currentPlayer, player2)

  test("base rules: play card of same rank"):
    val state0 = Values.threePlayers()
    val WithUntrustedRules(state, _) = RoundEngine.handle(state0, Rule.baseRules, Action.Play(player1, Card(Three, Spade)))
    assertEquals(state.discard.head, Card(Three, Spade))
    assertEquals(state.hands(player1).size, 4)
    assertEquals(state.currentPlayer, player2)

  test("base rules: penalty because out of turn"):
    val state0 = Values.threePlayers()
    val WithUntrustedRules(state, _) = RoundEngine.handle(state0, Rule.baseRules, Action.Play(player2, Card(Five, Diamond)))
    assertEquals(state.discard, state0.discard)
    assertEquals(state.hands(player2).size, 6)
    assert(state.hands(player2).contains(state0.stack.head))
    assertEquals(state.currentPlayer, state0.currentPlayer)

  test("base rules: penalty because invalid card"):
    val state0 = Values.threePlayers()
    val WithUntrustedRules(state, _) = RoundEngine.handle(state0, Rule.baseRules, Action.Play(player1, Card(Ace, Spade)))
    assertEquals(state.discard.head, state0.discard.head)
    assertEquals(state.hands(player1).size, 6)
    assert(state.hands(player1).contains(state0.stack.head))
    assertEquals(state.currentPlayer, state0.currentPlayer)

  test("base rules: draw instead of play"):
    val state0 = Values.threePlayers()
    val WithUntrustedRules(state, _) = RoundEngine.handle(state0, Rule.baseRules, Action.Draw(player1))
    assertEquals(state.discard.head, state0.discard.head)
    assertEquals(state.hands(player1).size, 6)
    assertEquals(state.currentPlayer, player2)

  test("base rules: anyone can draw anytime"):
    val state0 = Values.threePlayers()
    val WithUntrustedRules(state, _) = RoundEngine.handle(state0, Rule.baseRules, Action.Draw(player2))
    assertEquals(state.discard.head, state0.discard.head)
    assertEquals(state.hands(player1).size, 5)
    assertEquals(state.hands(player2).size, 6)
    assertEquals(state.currentPlayer, player1)

  test("base rules: winning round"):
    val state0 = Values.threePlayers(hand1 = Hand(Card(Two, Diamond)))
    val WithUntrustedRules(state, _) = RoundEngine.handle(state0, Rule.baseRules, Action.Play(player1, Card(Two, Diamond)))
    assertEquals(state.winner, Some(player1))

  test("base rules: draw last card"):
    val state0 = Values.threePlayers()
    val state1 = state0.copy(stack = Seq(state0.stack.head), discard = state0.stack.tail ++ state0.discard)
    val WithUntrustedRules(state, _) = RoundEngine.handle(state1, Rule.baseRules, Action.Draw(player1))
    assertEquals(state.stack.size, 35)
    assertEquals(state.discard, Seq(state1.discard.head))

  test("base rules: get last card as a penalty"):
    val state0 = Values.threePlayers()
    val state1 = state0.copy(stack = Seq(state0.stack.head), discard = state0.stack.tail ++ state0.discard)
    val WithUntrustedRules(state, _) = RoundEngine.handle(state1, Rule.baseRules, Action.Play(player2, Card(Five, Diamond)))
    assertEquals(state.stack.size, 35)
    assertEquals(state.discard, Seq(state1.discard.head))

  test("next player: when player plays out of turn"):
    val rule: Rule = Rule: state =>
      case _: Action.Play => Decision.ValidTurn
    val rules = rule +: Rule.baseRules
    val state0 = Values.threePlayers()
    val WithUntrustedRules(state, _) = RoundEngine.handle(state0, rules, Action.Play(player2, Card(Five, Diamond)))
    assertEquals(state.discard.head, Card(Five, Diamond))
    assertEquals(state.hands(player2).size, 4)
    assertEquals(state.currentPlayer, player3)

  test("reverse direction: on played card"):
    val rule: Rule = Rule: state =>
      case Action.Play(_, card) if card.rank == Ten => Decision.ReverseDirection
    val rules = rule +: Rule.baseRules
    val state0 = Values.threePlayers(currentPlayer = player3)
    val WithUntrustedRules(state, _) = RoundEngine.handle(state0, rules, Action.Play(player3, Card(Ten, Diamond)))
    assertEquals(state.discard.head, Card(Ten, Diamond))
    assertEquals(state.hands(player3).size, 4)
    assertEquals(state.direction, Direction.CounterClockWise)
    assertEquals(state.currentPlayer, player2)

  test("reverse direction: do not if invalid card"):
    val rule: Rule = Rule: state =>
      case Action.Play(_, card) if card.rank == Ten => Decision.ReverseDirection
    val rules = rule +: Rule.baseRules
    val state0 = Values.threePlayers(currentPlayer = player2)
    val WithUntrustedRules(state, _) = RoundEngine.handle(state0, rules, Action.Play(player2, Card(Ten, Heart)))
    assertEquals(state.discard, state0.discard)
    assertEquals(state.hands(player2).size, 6)
    assertEquals(state.direction, Direction.ClockWise)
    assertEquals(state.currentPlayer, player2)

  test("change direction: do not if out of turn"):
    val rule: Rule = Rule: state =>
      case Action.Play(_, card) if card.rank == Ten => Decision.ReverseDirection
    val rules = rule +: Rule.baseRules
    val state0 = Values.threePlayers()
    val WithUntrustedRules(state, _) = RoundEngine.handle(state0, rules, Action.Play(player3, Card(Ten, Diamond)))
    assertEquals(state.discard, state0.discard)
    assertEquals(state.hands(player3).size, 6)
    assertEquals(state.direction, Direction.ClockWise)
    assertEquals(state.currentPlayer, player1)
 */