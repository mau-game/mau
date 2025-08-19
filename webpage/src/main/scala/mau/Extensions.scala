package mau

import com.raquo.laminar.api.L.*
import mau.game.EventProjection
import mau.round.*
import mau.round.Action

extension (obs: Observable[Double])
  def px: Observable[String] = obs.map(_.px)
  def percent: Observable[String] = obs.map(_.percent)

extension (x: Double)
  def px: String = s"${x}px"
  def percent: String = s"${x}%"

extension (event: EventProjection)
  def message: String = event match
    case EventProjection.GameStarted(_) => "Game started."
    case EventProjection.PlayerEntered(_, player) => s"$player entered game."
    case EventProjection.PlayerExited(_, player) => s"$player exited game."
    case EventProjection.PlayerChecked(_, player) => s"$player checked in."
    case EventProjection.RoundStarted(_, firstPlayer) => s"Round started: first player is $firstPlayer."
    case EventProjection.RoundAction(_, effect) => effect.message
    case EventProjection.RoundWon(_, winner) => s"$winner won the round."
    case EventProjection.RoundStopped(_) => s"Round stopped."
    case EventProjection.RuleAdded(_, editor) => s"$editor added a new rule."
    case EventProjection.RuleRejected(_, editor) => s"The rule of $editor was rejected."
    case EventProjection.RulesReset(_, player) => s"$player reset all rules."

extension (effect: EffectProjection)
  def message: String = effect match
    case EffectProjection.Penalised(player, Some(card), _, _, reason) =>  s"$player received $card as a penalty for ${reason.message}."
    case EffectProjection.Penalised(player, None, _, _, reason) => s"$player received a penalty for ${reason.message}"
    case EffectProjection.CardPlayed(player, card) => s"$player played $card."
    case EffectProjection.CardDrawn(player, Some(card)) => s"$player drew $card."
    case EffectProjection.CardDrawn(player, None) => s"$player drew a card."
    case EffectProjection.ButtonClicked(player, button) => s"$player clicked $button"

extension (action: Action)
  def message: String = action match
    case Action.Draw => "drew card"
    case Action.Play(card) => s"played ${card.showLittle}"
    case Action.ClickButton(button) => s"clicked ${button.name}"
  
extension (reason: Reason)
  def message: String = reason match
    case Reason.InvalidCard => "Invalid Card"
    case Reason.InvalidButton => "Invalid Button"
    case Reason.NotClickButton(button) => s"Forgot Clicking ${button.name}"
    case Reason.OutOfTurn => "Out of Turn"
    case Reason.TooLongToPlay => "Too Long to Play"
  

extension (card: Card)
  def showLittle: String = s"${card.rank.showLittle}${card.suit.showLittle}"

extension (rank: Rank)
  def showLittle: String = rank match
    case Rank.Ace => "A"
    case Rank.Two => "2"
    case Rank.Three => "3"
    case Rank.Four => "4"
    case Rank.Five => "5"
    case Rank.Six => "6"
    case Rank.Seven => "7"
    case Rank.Eight => "8"
    case Rank.Nine => "9"
    case Rank.Ten => "10"
    case Rank.Jack => "J"
    case Rank.Queen => "Q"
    case Rank.King => "K"
  
extension (suit: Suit)
  def showLittle: String = suit match
    case Suit.Club => "♣"
    case Suit.Diamond => "♦"
    case Suit.Heart => "♥"
    case Suit.Spade => "♠"
