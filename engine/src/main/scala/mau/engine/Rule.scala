package mau.engine

import io.circe.Codec
import mau.edition.CompilationReport
import mau.game.RuleSuggestion
import mau.round.*

import java.net.URI

sealed trait Rule:
  def buttons: Seq[Button]
  def id: String

case class ExternalRule(uri: URI, name: String, buttons: Seq[Button], code: String) extends Rule derives Codec:
  def id: String = name

class InMemoryRule(
  val id: String,
  f: RulingState => PartialFunction[Action, Decision],
  code: Option[String],
  val buttons: Seq[Button],
  description: Option[String],
  // on the number of players
  val predicate: Int => Boolean
) extends Rule:
  def apply(state: RulingState): PartialFunction[Action, Decision] = f(state)

  def asSuggestion: Option[RuleSuggestion] = 
    for
      description <- description
      code <- code
    yield
      val fullCode =
        s"""|// ${description.replaceAll("\n", "\n// ")}
            |$code""".stripMargin
      RuleSuggestion(id, description, CompilationReport(fullCode, Seq.empty))

  def withDescription(description: String): InMemoryRule = copy(description = Some (description))

  def withPredicate(predicate: Int => Boolean): InMemoryRule = copy(predicate = predicate)

  private def copy(description: Option[String] = description, predicate: Int => Boolean = predicate): InMemoryRule =
    new InMemoryRule(id, f, code, buttons, description, predicate)

object InMemoryRule:
  def apply(
    id: String,
    f: RulingState => PartialFunction[Action, Decision],
    buttons: Seq[Button] = Seq.empty,
    code: Option[String] = None,
    description: Option[String] = None,
    predicate: Int => Boolean = _ => true
  ): InMemoryRule =
    new InMemoryRule(id, f, code, buttons, description, predicate)

object Rule:
  val defaultTurn: InMemoryRule = Macro.rule: state =>
    case action => if state.isExpectedPlayer then Decision.ValidTurn else Decision.OutOfTurn
  
  val invalidCard: InMemoryRule = Macro.rule: state =>
    case _: Action.Play => Decision.InvalidCard
  
  val invalidButton: InMemoryRule = Macro.rule: state =>
    case _: Action.ClickButton => Decision.InvalidButton
  
  val sameRank: InMemoryRule = Macro.rule: state =>
    case Action.Play(card) if state.discard.rank == card.rank => Decision.ValidCard
  
  val sameSuit: InMemoryRule = Macro.rule: state =>
    case Action.Play(card) if state.discard.suit == card.suit => Decision.ValidCard
  
  val lastCard: InMemoryRule = Macro.rule: state =>
    case Action.Play( _) if state.yourHandSize == 1 => Decision.MustClick("MAU")
  
  val winRound: InMemoryRule = Macro.rule: state =>
    case Action.ClickButton(Button("MAU")) => Decision.WinRound

  val haveANiceDay: InMemoryRule = Macro
    .rule( state => { case Action.Play(card) if card.rank == Rank.Seven => Decision.MustClick("HAVE A NICE DAY") })
    .withDescription("After playing a seven, you must click \"HAVE A NICE DAY\".")

  val diamondOnAny: InMemoryRule = Macro
    .rule(state => { case Action.Play(card) if card.suit.isDiamond => Decision.ValidCard })
    .withDescription("A diamond card may be played on top of any other card.")

  val noFaceOnFace: InMemoryRule = Macro
    .rule(state => { case Action.Play(card) if card.rank.isFace && state.discard.rank.isFace => Decision.InvalidCard })
    .withDescription("A face card cannot be played on top of another face card.")

  val aceAtAnyTurn: InMemoryRule = Macro
    .rule(state => { case Action.Play(card) if card.rank == Rank.Ace => Decision.ValidTurn })
    .withDescription("An ace may be played if it is not your turn.")
    .withPredicate(_ > 1)

  val tenReverseDirection: InMemoryRule = Macro
    .rule(state => { case Action.Play(card) if card.rank == Rank.Ten => Decision.ReverseDirection })
    .withDescription("Playing a ten reverses the direction of play.")
    .withPredicate(_ > 2)

  val colorToDirection: InMemoryRule = Macro
    .rule(state => { 
      case Action.Play(card) =>
        if card.isRed then Decision.TurnClockWise else Decision.TurnCounterClockWise
      }
    )
    .withDescription("After a red card, turn clockwise. After a black card, turn counter-clockwise.")
    .withPredicate(_ > 2)

  val clockwiseOnFace: InMemoryRule = Macro
    .rule(state => { case Action.Play(card) if card.isFace => Decision.TurnClockWise })
    .withDescription("After a face, turn clockwise.")
    .withPredicate(_ > 2)

  val mustChangeColor: InMemoryRule = Macro
    .rule(state => {
      case Action.Play(card) if card.suit != state.discard.suit && (card.isBlack == state.discard.isBlack) => 
        Decision.InvalidCard
      }
    )
    .withDescription(
      """|A spade cannot be played on a club, nor a club on a spade.
         |A heart cannot be played on a diamond, nor a diamond on a heart.""".stripMargin
    )

  val higherOnSmaller: InMemoryRule = Macro
    .rule(
      state => {
        case Action.Play(card) if card.suit == state.discard.suit && card.rank.value < state.discard.rank.value =>
          Decision.InvalidCard
        case Action.Play(card) if card.suit != state.discard.suit && card.rank.value < state.discard.rank.value =>
          Decision.ValidCard
      }
    )
    .withDescription(
      """|A smaller card cannot be played on a higher card of the same suit.
         |A smaller card may be played on a higher card of a different suit.""".stripMargin
    )

  val sameRankAtAnyTurn: InMemoryRule = Macro
    .rule(state => { case Action.Play(card) if card.rank == state.discard.rank => Decision.ValidTurn })
    .withDescription("A card of the same rank as the discard card may be played at any time, even if it is not your turn.")
    .withPredicate(_ > 1)

  val sameRankSwitchDirection: InMemoryRule = Macro
    .rule(state => { case Action.Play(card) if card.rank == state.discard.rank => Decision.ReverseDirection })
    .withDescription("Playing a card of the same rank as the discard card reverses the direction of play.")
    .withPredicate(_ > 2)

  val clickFaceOnFace: InMemoryRule = Macro
    .rule(
      state => {
        case Action.Play(card) if card.isFace =>
          card.rank match
            case Rank.Jack => Decision.MustClick("JACK")
            case Rank.Queen => Decision.MustClick("QUEEN")
            case Rank.King => Decision.MustClick("KING")
            case _ => ???
      }
    )
    .withDescription("After playing a face you must click the button corresponding to that face.")

  val clickInYourFaceOnAFace: InMemoryRule = Macro
    .rule(
      state => {
        case Action.Play(card) if !card.isFace && state.discard.isFace => Decision.MustClick("IN YOUR FACE")
      }
    )
    .withDescription("After playing a non-face card on top of a face you must click \"IN YOUR FACE\".")

  val playAnyCardOnASeven: InMemoryRule = Macro
    .rule(state => { case _: Action.Play if state.discard.rank == Rank.Seven => Decision.ValidCard })
    .withDescription("You may play any card on a seven.")

  val playOnFaceAtAnyTime: InMemoryRule = Macro
    .rule(state => { case Action.Play(_) if state.discard.isFace => Decision.ValidTurn })
    .withDescription("On top of a face, you may play at any time, even if it is not your turn.")
    .withPredicate(_ > 1)

  val clickYoloIfNotYourTurn: InMemoryRule = Macro
    .rule( state => { case Action.Play(_) if !state.isExpectedPlayer => Decision.MustClick("YOLO") })
    .withDescription("If you play a card but you are not the current player, you must click \"YOLO\".")
    .withPredicate(_ > 1)

  val canPlayIfSumIsTen: InMemoryRule = Macro
    .rule(state => { case Action.Play(card) if card.rank.value + state.discard.rank.value < 10 => Decision.ValidCard })
    .withDescription(
      """|You may play any card if its sum with the discard card is less than 10.
         |For example, you can play a 5♠ on a 2♦.""".stripMargin
    )

  val reverseBaseRules: InMemoryRule = Macro
    .rule(
      state => { 
        case Action.Play(card) =>
          if card.rank == state.discard.rank || card.suit == state.discard.suit then Decision.InvalidCard
          else Decision.ValidCard
      }
    )
    .withDescription(
      """|You cannot play a card if it has the same rank or suit as the discard card.
         |You may play it otherwise.""".stripMargin
    )

  // val notDrawOnJack: Rule = Rule(
  //   state => { case _: Action.Draw if state.discard.rank == Rank.Jack => Decision.InvalidDraw },
  //   description = Some("You cannot draw a card if the discard card is a jack.")
  // )

  val twoCardsSkip: InMemoryRule = Macro
    .rule(
      state => {
        case Action.ClickButton(button) if state.yourHandSize == 2 && button.name == "PASS" =>
          Decision.ValidButton
      }
    )
    .withDescription("When you have only 2 cards, you can click \"PASS\" instead of playing.")
    .withPredicate(_ > 1)

  val playCardIfLessThanCurrentPlayer: InMemoryRule = Macro
    .rule(
      state => {
        case Action.Play(_) if state.yourHandSize > state.expectedPlayerHandSize =>
          Decision.ValidTurn
      }
    )
    .withDescription("You can play if you have more cards than the current player, even if it is not your turn.")
    .withPredicate(_ > 1)

  val jackSkip: InMemoryRule = Macro
    .rule(
      state => {
        case Action.ClickButton(button) if button.name == "PASS" && state.discard.rank == Rank.Jack =>
          Decision.ValidButton
      }
    )
    .withDescription("On a jack you can click \"PASS\" instead of playing.")
    .withPredicate(_ > 1)

  val clickSuitOnChangeSuit: InMemoryRule = Macro
    .rule(
      state => {
        case Action.Play(card) if card.suit != state.discard.suit => 
          if card.suit.isDiamond then Decision.MustClick("DIAMOND")
          else if card.suit.isHeart then Decision.MustClick("HEART")
          else if card.suit.isClub then Decision.MustClick("CLUB")
          else Decision.MustClick("SPADE")
      }
    )
    .withDescription("After playing a card of a different suit you must click the button corresponding to that suit.")

  val clickReverseOnSix: InMemoryRule = Macro
    .rule(
      state => {
        case Action.Play(card) if card.rank == Rank.Six => Decision.MustClick("REVERSE")
        case Action.ClickButton(button) if button.name == "REVERSE" => Decision.ReverseDirection
      }
    )
    .withDescription(
      """|After playing a six, you must click "REVERSE".
         |Clicking "REVERSE" reverses the direction of play.""".stripMargin
    )
    .withPredicate(_ > 2)

  val notPlayEvenOnOdd: InMemoryRule = Macro
    .rule(
      state => {
        case Action.Play(card) if (card.isEven && state.discard.isOdd) || (card.isOdd && state.discard.isEven) =>
          Decision.InvalidCard
      }
    )
    .withDescription(
      """|You cannot play an even card on top of an odd card, nor an odd card on an even card.
         |Faces are neither considered even nor odd.""".stripMargin
    )

  val notPlayOnSpade: InMemoryRule = Macro
    .rule(
      state => {
        case Action.Play(_) if state.discard.isSpade =>
          if state.isExpectedPlayer then Decision.OutOfTurn else Decision.ValidTurn
      }
    )
    .withDescription("On a spade, you cannot play if you are the current player, but you can play if you are not the current player.")
    .withPredicate(_ > 1)

  val clickNeverEnough: InMemoryRule = Macro
    .rule(state => { case Action.Draw if state.yourHandSize >= 5 => Decision.MustClick("NEVER ENOUGH") })
    .withDescription("After drawing a card, if you have more than 5 cards, you must click \"NEVER ENOUGH\"")

  val anyCard: InMemoryRule = Macro.rule(state => { case Action.Play(_) => Decision.ValidCard })

  val anyClick: InMemoryRule = Macro.rule(state => { case Action.Play(_) => Decision.MustClick("BUTTON") })

  val anyTurn: InMemoryRule = Macro.rule(state => { case Action.Play(_) => Decision.ValidTurn })

  val baseRules: Seq[InMemoryRule] = Seq(
    //anyCard,
    //anyClick,
    //anyTurn,
    haveANiceDay,
    winRound,
    lastCard,
    sameRank,
    sameSuit,
    invalidButton,
    invalidCard,
    defaultTurn
  )

  val additionalRules: Set[InMemoryRule] = Set(
    diamondOnAny,
    noFaceOnFace,
    aceAtAnyTurn,
    tenReverseDirection,
    colorToDirection,
    clockwiseOnFace,
    mustChangeColor,
    higherOnSmaller,
    sameRankAtAnyTurn,
    sameRankSwitchDirection,
    clickFaceOnFace,
    clickInYourFaceOnAFace,
    playAnyCardOnASeven,
    playOnFaceAtAnyTime,
    clickYoloIfNotYourTurn,
    canPlayIfSumIsTen,
    reverseBaseRules,
    twoCardsSkip,
    playCardIfLessThanCurrentPlayer,
    jackSkip,
    clickSuitOnChangeSuit,
    clickReverseOnSix,
    notPlayEvenOnOdd,
    notPlayOnSpade,
    clickNeverEnough
  )

  def suggestions(numberOfPlayers: Int): Map[String, InMemoryRule] = 
    additionalRules.filter(_.predicate(numberOfPlayers)).map(r => r.id -> r).toMap
