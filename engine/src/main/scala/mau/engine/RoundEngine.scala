package mau.engine

import mau.exec.*
import mau.round.*

import java.time.Instant
import scala.util.Random

object RoundEngine:
  def init(players: Seq[Player], buttons: Seq[Button]): RoundState =
    val shuffledCards = Random.shuffle(Card.allCards)
    val hands = players.zip(shuffledCards.take(players.size * 5).grouped(5).map(Hand(_))).toMap
    val discard = shuffledCards(players.size * 5)
    val remainingCards = shuffledCards.drop(players.size * 5 + 1)
    RoundState(
      buttons,
      remainingCards,
      Seq(discard),
      players,
      hands,
      Direction.ClockWise,
      MustAct.play(players.head),
      winner = None,
      effect = None
    )


class RoundEngine(ruleExecutor: RuleExecutor):
  def handle(state: RoundState, rules: Seq[Rule], action: Action, player: Player): WithUntrustedRules[RoundState] =
    state.validate(action, player)
    decide(state.ruling(player), rules, action).map: decisions =>
      val nextDirection = computeNextDirection(state, decisions)
      
      def finalize(s0: RoundState): RoundState =
        val mustClick = getMustClick(player, decisions)
        if isWinning(decisions) then s0.win(player)
        else mustClick match 
          case Some(mustClick) => s0.withDirection(nextDirection).withMustAct(mustClick)
          case None => s0.withDirection(nextDirection).endTurn(player)

      action match
        case Action.Play(card) =>
          val state0 = state.playCard(player, card)
          val penalty =
            if isOutOfTurn(decisions) then Some(Reason.OutOfTurn)
            else if isInvalidCard(decisions) then Some(Reason.InvalidCard)
            else None
          penalty match
            case Some(reason) => state.penalise(player, Some(action), state0.effect, reason)
            case None => finalize(state0)
        
        case Action.Draw =>
          val state0 = state.drawCard(player)
          if isOutOfTurn(decisions) then state0.penalise(player, Some(action), state0.effect, Reason.OutOfTurn)
          else finalize(state0)
        
        case Action.ClickButton(button) =>
          val state0 = state.clickButton(player, button)
          state.mustAct match
            case mustClick: MustAct.Click if mustClick.player == player  =>
              val penalty =
                if mustClick.buttons.contains(button) then None
                else Some(Reason.InvalidButton)
              penalty match
                case Some(reason) => state0.penalise(player, Some(action), state0.effect, reason)
                case None =>
                  if isWinning(decisions) then state0.win(player)
                  else state0.withDirection(nextDirection).endClick(player, button)
            
            case _ =>
              val penalty = 
                if isOutOfTurn(decisions) then Some(Reason.OutOfTurn)
                else if isInvalidButton(decisions) then Some(Reason.InvalidButton)
                else None
              
              penalty match
                case Some(reason) => state0.penalise(player, Some(action), state0.effect, reason)
                case None => finalize(state0)
  end handle

  def isAllowed(state: RoundState, action: Action, player: Player): Boolean =
    state.mustAct.isPlay || (action.isInstanceOf[Action.ClickButton] && state.mustAct.isClick(player))
  
  def tick(state: RoundState, rules: Seq[Rule], now: Instant): Option[RoundState] =
    Option.when(state.mustAct.isOverdue(now)):
      state.mustAct match
        case mustClick: MustAct.Click =>
          val button = mustClick.buttons.head
          state
            .penalise(mustClick.player, None, None, Reason.NotClickButton(button))
            .endClick(mustClick.player, button)
        case mustPlay: MustAct.Play =>
          state
            .penalise(mustPlay.player, None, None, Reason.TooLongToPlay)
            .endTurn(mustPlay.player)

  def addPlayer(state: RoundState, player: Player): RoundState =
    state.addPlayer(player)

  def removePlayer(state: RoundState, player: Player): RoundState =
    if state.mustAct.player == player then state.endTurn(player).removePlayer(player)
    else state.removePlayer(player)

  private def decide(state: RulingState, rules: Seq[Rule], action: Action): WithUntrustedRules[Seq[Decision]] =
    rules.foldLeft(WithUntrustedRules(Seq.empty[Decision])):
      case (acc, rule: InMemoryRule) => acc.map(_ ++ rule(state).unapply(action))
      case (acc, rule: ExternalRule) =>
        for
          decisions <- acc
          decisionOpt <- ruleExecutor.run(rule, state, action)
        yield decisions ++ decisionOpt

  private def isOutOfTurn(decisions: Seq[Decision]): Boolean =
    decisions
      .collectFirst:
        case Decision.ValidTurn   => false
        case Decision.OutOfTurn => true
      .getOrElse(false)

  private def isInvalidCard(decisions: Seq[Decision]): Boolean =
    decisions
      .collectFirst:
        case Decision.ValidCard   => false
        case Decision.InvalidCard => true
      .getOrElse(false)

  private def isInvalidButton(decisions: Seq[Decision]): Boolean =
    decisions
      .collectFirst:
        case Decision.ValidButton => false
        case Decision.InvalidButton => true
      .getOrElse(false)

  private def isWinning(decisions: Seq[Decision]): Boolean =
    decisions
      .collectFirst:
        case Decision.WinRound => true
      .getOrElse(false)

  private def getMustClick(player: Player, decisions: Seq[Decision]): Option[MustAct] =
    val buttons = decisions.collect { case Decision.MustClick(button) => button }
    Option.when(buttons.nonEmpty)(MustAct.click(player, buttons))

  private def computeNextDirection(state: RoundState, decisions: Seq[Decision]): Direction =
    val initial = decisions
      .collectFirst:
        case Decision.TurnClockWise => Direction.ClockWise
        case Decision.TurnCounterClockWise => Direction.CounterClockWise
      .getOrElse(state.direction)
    val reverse = decisions.count(_ == Decision.ReverseDirection) % 2 == 1
    if reverse then initial.reverse else initial
