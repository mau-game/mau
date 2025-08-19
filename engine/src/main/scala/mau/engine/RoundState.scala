package mau.engine

import mau.round.*

import scala.util.Random

final case class RoundState(
  buttons: Seq[Button],
  stack: Seq[Card],
  discard: Seq[Card],
  players: Seq[Player],
  hands: Map[Player, Hand],
  direction: Direction,
  mustAct: MustAct,
  winner: Option[Player],
  effect: Option[Effect]
):
  def ruling(player: Player): RulingState =
    RulingState(
      discard.head,
      yourHandSize = hands(player).size,
      expectedPlayerHandSize = hands(mustAct.player).size,
      player == mustAct.player,
      direction
    )

  def removePlayer(player: Player): RoundState =
    val nextAct =
      if mustAct.player == player then MustAct.play(nextPlayer(player))
      else mustAct
    val hand = hands.get(player).map(_.cards).getOrElse(Seq.empty)
    copy(
      stack = Random.shuffle(stack ++ hand),
      players = players.filterNot(_ == player),
      hands = hands - player,
      mustAct = nextAct,
      effect = None
    )

  def addPlayer(player: Player): RoundState =
    copy(players = players :+ player, hands = hands + (player -> Hand()))
      .drawCard(player)
      .drawCard(player)
      .drawCard(player)
      .drawCard(player)
      .drawCard(player)

  def validate(action: Action, player: Player): Unit =
    assert(hands.contains(player))
    assert(players.contains(player))
    action match
      case Action.Play(card) =>
        assert(mustAct.isPlay)
        assert(hands(player).contains(card))
      case Action.Draw =>
        assert(mustAct.isPlay)
      case Action.ClickButton(button) => 
        assert(mustAct.isPlay || mustAct.isClick(player))

  def playCard(player: Player, card: Card): RoundState =
    copy(
      discard = card +: discard,
      hands = takeCard(player, card),
      effect = Some(Effect.CardPlayed(player, card)),
    )

  def drawCard(player: Player): RoundState =
    val state0 = ensureHandLimit(player)
    val (state1, card) = state0.pickCard
    state1.copy(
      hands = state1.giveCard(player, card),
      effect = Some(Effect.CardDrawn(player, card))
    )

  def clickButton(player: Player, button: Button): RoundState =
    copy(effect = Some(Effect.ButtonClicked(player, button)))

  def penalise(player: Player, action: Action, reason: Reason): RoundState =
    val (state0, effect) = action match
      case Action.ClickButton(button) => (this, Effect.ButtonClicked(player, button))
      case Action.Draw =>
        val state0 = ensureHandLimit(player)
        val (state1, card) = state0.pickCard
        (state1.copy(hands = state1.giveCard(player, card)), Effect.CardDrawn(player, card))
      case Action.Play(card) => (this, Effect.CardPlayed(player, card))
    state0.penalise(player, Some(action), Some(effect), reason)
    
  def penalise(player: Player, action: Option[Action], effect: Option[Effect], reason: Reason): RoundState =
    val state0 = ensureHandLimit(player)
    val (state1, card) = state0.pickCard
    state1.copy(
      hands = state1.giveCard(player, card),
      effect = Some(Effect.Penalised(player, card, action, effect, reason)),
      mustAct = mustAct.reset,
    )
  
  def win(player: Player): RoundState = copy(winner = Some(player))

  def withMustAct(mustAct: MustAct) = copy(mustAct = mustAct)
  def withDirection(direction: Direction) = copy(direction = direction)

  private def nextPlayer(player: Player): Player =
    val orderedPlayers = direction match
      case Direction.ClockWise        => Stream.continually(players).flatten
      case Direction.CounterClockWise => Stream.continually(players.reverse).flatten
    orderedPlayers.dropWhile(_ != player).tail.head

  def endTurn(player: Player): RoundState =
    copy(mustAct = MustAct.play(nextPlayer(player)))
  
  def endClick(player: Player, button: Button): RoundState =
    mustAct match
      case mustClick: MustAct.Click if mustClick.player == player =>
        if mustClick.buttons.size > 1 then copy(mustAct = MustAct.click(player, mustClick.buttons.filter(_ != button)))
        else endTurn(player)
      case _ => this

  private def ensureHandLimit(player: Player): RoundState =
    val hand = hands(player)
    if hand.size >= 10 then
      val card = Random.shuffle(hand.cards).head
      copy(discard = discard :+ card, hands = takeCard(player, card))
    else this

  private def pickCard: (RoundState, Card) =
    if stack.size == 1 then (copy(stack = Random.shuffle(discard.tail), discard = Seq(discard.head)), stack.head)
    else (copy(stack = stack.tail), stack.head)

  private def takeCard(player: Player, card: Card): Map[Player, Hand] =
    hands.updated(player, hands(player).remove(card))

  private def giveCard(player: Player, card: Card): Map[Player, Hand] =
    hands.updated(player, hands(player).add(card))

  private def giveCards(player: Player, cards: Card*): Map[Player, Hand] =
    hands.updated(player, hands(player).add(cards*))
