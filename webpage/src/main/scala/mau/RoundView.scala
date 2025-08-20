package mau

import com.raquo.airstream.ownership.Owner
import com.raquo.laminar.api.L.*
import com.raquo.laminar.inserters.DynamicInserter
import com.raquo.laminar.nodes.ReactiveElement
import com.raquo.laminar.nodes.ReactiveHtmlElement
import mau.game.StateProjection.View.Round
import mau.round.*
import mau.round.EffectProjection.*
import mau.util.Animation
import org.scalajs.dom

import scala.scalajs.js

class RoundView(you: Player, observer: Observer[Action], $mustAct: Signal[MustActProjection], exitGame: Observer[Unit]):
  def apply($round: Signal[Round]): HtmlElement =
    div(
      idAttr := "round",
      className <-- $mustAct.map:
        case MustActProjection.Click(p) => if p == you then "must-click" else "must-wait"
        case _ => "",
      navbar(exitGame),
      div(
        idAttr := "round-frame",
        inContext: ctx =>
          val $frameSize = EventStream.periodic(100).toSignal(0).mapTo(Size(ctx.ref.getBoundingClientRect())).distinct
          val $layout = $round.combineWithFn($frameSize)(RoundLayout(you, _, _))
          val $animations = computeAnimations($round, $frameSize)
          Seq(
            otherPlayers($round, $layout, $animations),
            penaltyBox($round, $layout),
            stackAndDiscard($round, $layout, $animations),
            mainPlayer($round, $layout, $animations)
          )
      ),
      buttonSection($round)
    )

  private def computeAnimations($round: Signal[Round], $frameSize: Signal[Size]): Signal[CardAnimations] =
    $round
      .foldLeft(init => (Option.empty[Round], init)):
         case ((_, prev), next) => (Some(prev), next)
      .withCurrentValueOf($frameSize)
      .map:
        case (Some(previous), current, frameSize) =>
          val prevLayout = RoundLayout(you, previous, frameSize)
          val curLayout = RoundLayout(you, current, frameSize)
          CardAnimations(you, prevLayout, curLayout, current.effect)
        case (None, _, _) => CardAnimations.empty // TODO should we compute animations on first load


  private def otherPlayers($round: Signal[Round], $layout: Signal[RoundLayout], $animations: Signal[CardAnimations]): Modifier[HtmlElement] =
    val $effect = $round.map(_.effect)
    val $knownCards: Signal[Map[(Player, Int), Card]] = $round
      .map: round =>
        round.effect
          .collect { case Penalised(p, None, Some(Action.Play(card)), _, _) => (p, card) }
          .map: (player, card) =>
            // play the last but one card
            // get the last card as penalty
            val i = round.otherPlayers.toMap.apply(player) - 2
            (player, i) -> card
          .toMap
      .distinct
    Seq(
      children <-- $round.map(_.otherPlayers).split((p, _) => p)({case (player, _, _) => otherPlayerProfile(player, $layout)}),
      children <-- $round
        .map: round =>
          for
            (player, handSize) <- round.otherPlayers
            i <- 0.until(handSize)
          yield (player, i)
        .split(identity):
          case ((player, i), _, _) => otherPlayerCard(i, player, $knownCards, $layout, $animations),
      children <-- $round.map(_.otherPlayers).split((p, _) => p)({case (player, _, _) => otherMessageBox(player, $effect, $layout)}),
    ) 

  private def penaltyBox($round: Signal[Round], $layout: Signal[RoundLayout]): Modifier[HtmlElement] =
    val $effect = $round.map(_.effect).changes
    val $penalty = $effect.collect { case Some(p: EffectProjection.Penalised) => p } 
    val $penaltyBox = $layout.map(_.penalty)
    child <-- $penalty.withTiming(3000).map:
      case Some(EffectProjection.Penalised(player, _, action, _, reason)) =>
        div(
          idAttr := "penalty",
          h2("PENALTY!"),
          p(player.toString, className := s"player-${player.id}"),
          action.map(a => p(a.message)),
          p(reason.message, className := "penalty-reason"),
          left <-- $penaltyBox.map(_.left.px),
          bottom <-- $penaltyBox.map(_.bottom.px),
          width <-- $penaltyBox.map(_.width.px)
        )
      case None => emptyNode
  
  private def stackAndDiscard($round: Signal[Round], $layout: Signal[RoundLayout], $animations: Signal[CardAnimations]): Modifier[HtmlElement] =
    Seq(
      div(
        idAttr := "stack",
        className := "card",
        backFace(isDrawable = true),
        onClick.map(_ => Action.Draw) --> observer,
        withPosition("stack", $layout),
        withAnimation("stack", $animations)
      ),
      children <-- $round
        .map(_.discard)
        .split(identity):
          case (card, _, _) =>
            val id = s"discard-${card.id}"
            div(
              idAttr := "discard",
              idAttr := id,
              className := "card",
              frontAndBackFaces(card),
              withAnimation(id, $animations),
              withPosition(id, $layout),
            )
    )

  private def mainPlayer($round: Signal[Round], $layout: Signal[RoundLayout], $animations: Signal[CardAnimations]): Modifier[HtmlElement] =
    val $cards = $round.map(_.hand.map(_.cards).getOrElse(Seq.empty)) 
    Seq(
      children(
        div(
          idAttr := s"player-${you.id}-profile",
          className := "player-profile",
          className <-- $mustAct.map:
            case MustActProjection.Play(p) if p == you => "must-play"
            case MustActProjection.Click(p) if p == you => "must-click"
            case _ => "must-wait", // Someone else must act
          withPosition(s"player-${you.id}-profile", $layout)
        ),
        yourMessages($round, $layout)
      ) <-- $round.map(_.hand.isDefined).distinct,
      children <-- $round
        .map(_.hand.map(_.cards).getOrElse(Seq.empty))
        .split(identity):
          case (card, _, _) =>
            val clickBus = EventBus[Unit]()
            val playStream =
              clickBus
                .events
                .flatMapSwitch:
                  _ => $mustAct.map:
                    case MustActProjection.Click(_) => None
                    case _ => Some(Action.Play(card))
                .collectSome
            div(
              idAttr := card.id,
              className := "card",
              frontAndBackFaces(card, isPlayable = true),
              onClick.mapTo(()) --> clickBus,
              playStream --> observer,
              withPosition(card.id, $layout),
              withAnimation(card.id, $animations),
            )
    )

  private def yourMessages($round: Signal[Round], $layout: Signal[RoundLayout]): Div =
    val $messages = $round.map(_.effect).changes.collect:
      case Some(EffectProjection.ButtonClicked(p, button)) if p == you => button.name
      case Some(EffectProjection.Penalised(p, _, Some(Action.ClickButton(button)), _, _)) if p == you => button.name
    val $bottom = $layout.map: layout =>
      layout.mainPlayer.map(_.messageBox).collect({ case MessageBox.Center(y) => y.px }).getOrElse("")
    div(
      idAttr := "your-message-box",
      className := "message-box",
      children <-- accumulate($messages, 2000).split((_, i) => i):
        case (_, (msg, _), _) => p(className := "message", msg),
      bottom <-- $bottom.distinct
    )

  def accumulate[T](s: EventStream[T], timeMs: Int): Signal[Seq[(T, Int)]] =
    var i = -1
    val sWithIndex = s.map { x => i += 1; (x, i) }
    EventStream.merge(zipWithIndex(s).map(Some(_)), s.delay(timeMs).mapTo(None))
      .foldLeft(Seq.empty[(T, Int)]):
        case (xs, Some(x)) => x +: xs
        case (xs, None) => xs.dropRight(1)

  def zipWithIndex[T](s: EventStream[T]): EventStream[(T, Int)] =
    var i = -1
    s.map { x => i += 1; (x, i) }

  private def buttonSection($round: Signal[Round]): Div =
    div(
      idAttr := "button-section",
      div(
        className := "buttons-container",
        children <-- $round.distinctBy(_ => ()).map: round =>
          round.buttons.map: b =>
            button(
              className := "button",
              b.name,
              disabled <-- $mustAct.map:
                case MustActProjection.Click(p) if p != you => true
                case _ => false,
              onClick.map(_ => Action.ClickButton(b)) --> observer
            )
      )
    )


  private def formatHandSize(handSize: Int): String =
    handSize match
      case 0 => "Empty hand"
      case 1 => "One card"
      case x => s"$x cards"

  private def otherPlayerCard(
    i: Int,
    player: Player,
    $knownCards: Signal[Map[(Player, Int), Card]],
    $layout: Signal[RoundLayout],
    $animations: Signal[CardAnimations]
  ): Div =
    val id = s"player-${player.id}-card-$i"
    div(
      idAttr := id,
      className := "card",
      children <-- $knownCards.map(_.get((player, i))).distinct.map:
        case Some(card) => frontAndBackFaces(card, isFlipped = true)
        case None =>  Seq(backFace(isDrawable = false)),
      withPosition(id, $layout),
      withAnimation(id, $animations),
    )

  private def otherPlayerProfile(player: Player, $layout: Signal[RoundLayout]): Image =
    val id = s"player-${player.id}-profile"
    img(
      idAttr := id,
      className := "player-profile",
      className <-- $mustAct.map:
        case MustActProjection.Play(p) if p == player => "must-play"
        case MustActProjection.Click(p) if p == player => "must-click"
        case _ => "must-wait", // Someone else must act
      src := Assets.Avatars(player.user.avatar),
      alt := s"Player ${player.id} picture",
      withPosition(id, $layout)
    )

  private def otherMessageBox(player: Player, $effect: Signal[Option[EffectProjection]], $layout: Signal[RoundLayout]): Div =
    val $messages = $effect.changes.collect:
      case Some(EffectProjection.ButtonClicked(p, button)) if p == player => button.name
      case Some(EffectProjection.Penalised(p, _, Some(Action.ClickButton(button)), _, _)) if p == player => button.name
    val $messageBox = $layout.map(_.otherPlayers.get(player).map(_.messageBox)).distinct
    val $top = $messageBox.map:
      case Some(MessageBox.Left(_, top)) => top.px
      case Some(MessageBox.Right(_, top)) => top.px
      case _ => ""
    val $left = $messageBox.map:
      case Some(MessageBox.Left(left, _)) => left.px
      case _ => ""
    val $right = $messageBox.map:
      case Some(MessageBox.Right(right, _)) => right.px
      case _ => ""
    val $alignItems = $messageBox.map:
      case Some(_: MessageBox.Left) => "flex-start"
      case Some(_: MessageBox.Right) => "flex-end"
      case _ => ""
    val $messageClass = $messageBox.map:
      case Some(_: MessageBox.Left) => "message-left"
      case Some(_: MessageBox.Right) => "message-right"
      case _ => ""
    val alignItems = new StyleProp[String]("align-items")
    div(
      className := "other-message-box",
      children <-- accumulate($messages, 2000).split((_, i) => i):
        case (_, (msg, _), _) => p(className := "message", className <-- $messageClass, msg),
      top <-- $top.distinct,
      left <-- $left.distinct,
      right <-- $right.distinct,
      alignItems <-- $alignItems.distinct
    )

  private def frontAndBackFaces(card: Card, isPlayable: Boolean = false, isDrawable: Boolean = false, isFlipped: Boolean = false): Seq[Div] =
    Seq(frontFace(card, isPlayable, isBehind = isFlipped), backFace(isDrawable, isBehind = !isFlipped))

  private def frontFace(card: Card, isPlayable: Boolean, isBehind: Boolean): Div =
    div(
      className := "card-face",
      if isPlayable then className := "playable" else emptyNode,
      if isBehind then transform := "rotateY(180deg)" else emptyNode,
      div(
        className := "card-front",
        div(card.rank.showLittle, className := Seq("card-rank", if card.suit.isBlack then "black" else "red")),
        div(card.suit.showLittle, className := Seq("card-suit", if card.suit.isBlack then "black" else "red")),
      )
    )

  private def backFace(isDrawable: Boolean, isBehind: Boolean = false): Div =
    div(
      className := "card-face",
      if isDrawable then className := "drawable" else emptyNode,
      if isBehind then transform := "rotateY(180deg)" else emptyNode,
      div(className := "card-back"),
    )

  private def withPosition(id: String, $layout: Signal[RoundLayout]): Modifier[HtmlElement] =
    val $elem = $layout.map(_.allElements.getOrElse(id, ElementLayout.empty(id))).distinct
    Seq(
      position.absolute,
      left <-- $elem.map(_.left.px),
      top <-- $elem.map(_.top.px),
      width <-- $elem.map(_.width.px),
      height <-- $elem.map(_.height.px),
      transform <-- $elem.map(e => s"scale(${e.scale}) rotate(${e.rotation}deg)"),
      zIndex <-- $elem.map(_.zIndex)
    )

  private def withAnimation(id: String, $animations: Signal[CardAnimations], debug: Boolean = false): Modifier[Div] =
    val $animation = $animations.map(_.get(id)).map:
      case Some(animation) => 
        if debug then org.scalajs.dom.console.log(animation.jsKeyframes)
        Some(animation)
      case None => None
    inContext[Div]: ctx =>
      Binder[Div]: div =>
        ReactiveElement.bindSubscriptionUnsafe(div): mountContext =>
          given Owner = mountContext.owner
          $animation.foreach:
            case Some(animation) => ctx.ref.asInstanceOf[js.Dynamic].animate(animation.jsKeyframes, animation.jsOptions)
            case None => ()
