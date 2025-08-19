package mau.round

import mau.*
import mau.round.Action
import mau.util.Animation
import mau.util.Keyframe
import mau.util.Keyframe.*

type CardAnimations = Map[String, Animation]

object CardAnimations:
  def empty: CardAnimations = Map.empty

  def apply(you: Player, previous: RoundLayout, next: RoundLayout, effect: Option[EffectProjection]): CardAnimations =
    effect match
      case None => translations(next.allCards, previous)
      case Some(effect) => apply(you, previous, next, effect)

  def apply(you: Player, previous: RoundLayout, next: RoundLayout, effect: EffectProjection): CardAnimations =
    def translations0(cards: Map[String, ElementLayout]) = translations(cards, previous)
    import EffectProjection.*

    effect match
      case Penalised(_, Some(penaltyCard), _, effect, _) =>
        def playCard(card: Card) = 
          val anim = roundTrip(previous.hand.get(card.id), next.discard.head, next.hand.get(card.id), flip = false)
          (Some(card.id -> anim), 900)
        def drawCard(card: Card) =
          val anim = moved(previous.stack, next.hand.get(card.id), flip = true)
          (Some(card.id -> anim), 300)
        val (actionAnim, delay) = effect match
          case Some(EffectProjection.CardPlayed(_, card)) => playCard(card)
          case Some(EffectProjection.CardDrawn(_, Some(card))) => drawCard(card)
          case _ => (None, 0)
        val penaltyAnim = moved(previous.stack, next.hand.get(penaltyCard.id), flip = true).delayed(delay)
        val delayedTranslations = translations0(next.hand.get.elems.toMap).mapValues(_.delayed(delay)).toMap
        delayedTranslations ++ actionAnim + (penaltyCard.id -> penaltyAnim)
      
      case Penalised(player, None, action, _, _) =>
        val nextHand = next.otherPlayers(player).cards.toSeq
        val (cardId, card) = nextHand.toSeq(nextHand.size - 2)
        def playCard = 
          val anim = roundTrip(previous.otherPlayers(player).cards(cardId), next.discard.head, card, flip = true)
          (Some(cardId -> anim), 900)
        def drawCard =
          val anim = moved(previous.stack, card)
          (Some(cardId -> anim), 300)
        val (actionAnim, delay) = action match
          case Some(Action.Play(_)) => playCard
          case Some(Action.Draw) => drawCard
          case _ => (None, 0)
        val (penaltyId, penaltyCard) = next.lastCard(player)
        val penaltyAnim = moved(previous.stack, penaltyCard).delayed(delay)
        val delayedTranslations = translations0(next.otherPlayers(player).cards.toMap).mapValues(_.delayed(delay)).toMap
        delayedTranslations ++ actionAnim + (penaltyId -> penaltyAnim)
      
      case CardPlayed(p, card) if p == you =>
        translations0(next.allCards) +
          (s"discard-${card.id}" -> moved(previous.hand.get(card.id), next.discard.head))
      
      case CardPlayed(otherPlayer, card) =>
        val (_, prevCard) = previous.lastCard(otherPlayer)
        translations0(next.allCards) +
          (s"discard-${card.id}" -> moved(prevCard, next.discard.head, flip = true))
      
      case CardDrawn(_, Some(card)) =>
        translations0(next.allCards) +
          (card.id -> moved(previous.stack, next.hand.get(card.id), flip = true))
      
      case CardDrawn(otherPlayer, None) =>
        val (id, card) = next.lastCard(otherPlayer)
        translations0(next.allCards) + (id -> moved(previous.stack, card))

      case _ => Map.empty
  end apply
  
  private def translations(cards: Map[String, ElementLayout], previous: RoundLayout): Map[String, Animation] =
      for
        (card, next) <- cards
        previous <- previous.allCards.get(card)
        if previous != next
      yield card -> translation(previous, next)

  private def translation(start: ElementLayout, dest: ElementLayout): Animation =
    Animation(
      Keyframe(transformation(start, dest, false), ZIndex(start.zIndex), Easing("ease-in-out")),
      Keyframe(destination(dest), Easing("ease-in-out"))
    )

  private def moved(start: ElementLayout, dest: ElementLayout, flip: Boolean = false): Animation =
    Animation(
      Keyframe(transformation(start, dest, flip), ZIndex(100), Easing("ease-in-out")),
      Keyframe(destination(dest), Easing("ease-in-out"))
    )

  private def roundTrip(start: ElementLayout, middle: ElementLayout, dest: ElementLayout, flip: Boolean = false): Animation =
    Animation(
      Keyframe(transformation(start, dest, false), Easing("ease-in-out")),
      // here zindex needs to be above the penalty, which is a delayed moved animation
      Keyframe(transformation(middle, dest, flip), ZIndex(200), Easing("ease-in-out"), Offset(0.33f)),
      Keyframe(transformation(middle, dest, flip), ZIndex(200), Easing("ease-in-out"), Offset(0.5f)),
      Keyframe(transformation(start, dest, false), Easing("ease-in-out"), Offset(0.66f)),
      Keyframe(destination(dest), Easing("ease-in-out"))
    )
    .withDurationMs(1200)

  private def transformation(start: ElementLayout, dest: ElementLayout, flip: Boolean): Keyframe.Property =
    val translate = Option.when(start.center != dest.center)(Translate(start.x - dest.x, start.y - dest.y))
    val rotateY = Option.when(flip)(RotateY(180))
    Transform(translate.toSeq ++ Seq(Scale(start.scale), Rotate(start.rotation)) ++ rotateY)

  private def destination(dest: ElementLayout): Keyframe.Property =
    Transform(Seq(Scale(dest.scale), Rotate(dest.rotation)))

