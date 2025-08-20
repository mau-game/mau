package mau.round

import mau.game.StateProjection.View

object MobileLayout:
  private class Params(scale: Double):
    extension (x: Int)
      private def scaled: Int = (x * scale).toInt

    val cardSize = Size(84.scaled, 120.scaled)
    val discardStackSpacing = 6.scaled
    object MainPlayer:
      val cardRowSize: Int = 8
      val cardHorizontalSpacing: Int = 42.scaled
      val cardVerticalSpacing: Int = 60.scaled
      val yOffset: Int = 40.scaled

    object OtherPlayer:
      val profileSize = Size(66.scaled, 66.scaled)
      val cardScale = 2.0/3
      val cardHorizontalSpacing: Int = -28.scaled
      val cardVerticalSpacing: Int = 20.scaled
  end Params

  private object Params:
    def apply(frameSize: Size): Params =
      val scale = Math.max(2.0 / 3, Math.min(frameSize.width.toDouble / 416, frameSize.height.toDouble / 612))
      new Params(scale)

  def apply(you: Player, round: View.Round, frameSize: Size): RoundLayout =
    val params = Params(frameSize)
    import params.*
    val otherPlayers = round.otherPlayers.zipWithIndex.map:
      case ((player, handSize), i) =>
        val origin = otherPlayerOrigin(i, round.otherPlayers.size)
        player -> otherPlayerLayout(player, handSize, origin, frameSize, params)
    val penalty = PenaltyBox(left = frameSize.width / 4, bottom = 2 * frameSize.height / 3 + cardSize.height / 2, width = frameSize.width / 2)
    val stackPos = Position((frameSize.width - discardStackSpacing - cardSize.width) / 2 , frameSize.height / 3)
    val discardPos = Position((frameSize.width + discardStackSpacing + cardSize.width) / 2, frameSize.height / 3)
    val stack = ElementLayout("stack", stackPos, cardSize, scale = 1.0, zIndex = 1, rotation = 0)
    val discard = round.discard.zipWithIndex.map: (card, i) =>
      ElementLayout(s"discard-${card.id}", discardPos, cardSize, scale = 1.0, zIndex = round.discard.size - i, rotation = 0)
    val mainPlayer = round.hand.map(mainPlayerLayout(you, _, frameSize, params))
    RoundLayout(otherPlayers.toMap, penalty, stack, discard, mainPlayer)

  private def mainPlayerLayout(you: Player, hand: Hand, frameSize: Size, params: Params): PlayerLayout =
    import params.cardSize
    import params.MainPlayer.*
    val relativePositions = 0.until(hand.size).map: i =>
      val row = i / cardRowSize
      val column = i % cardRowSize
      val horizontalOffset = if row % 2 == 1 then (cardHorizontalSpacing / 2) else 0
      Position(column * cardHorizontalSpacing + horizontalOffset, row * cardVerticalSpacing)
    val maxX = relativePositions.map(_.x).maxOption.getOrElse(0)
    val maxY = relativePositions.map(_.y).maxOption.getOrElse(0)
    val translateX = (frameSize.width - maxX) / 2
    val translateY = (frameSize.height * 5 - maxY * 3) / 6 + yOffset
    val positions = relativePositions.map(_.translate(translateX, translateY))
    val cards = hand.cards.zip(positions).zipWithIndex.map:
      case ((card, pos), i) => ElementLayout(card.id, pos, cardSize, scale = 1.0, zIndex = i + 10, rotation = 0)
    val profile = ElementLayout(
      s"player-${you.id}-profile",
      Position(frameSize.width / 2, frameSize.height * 6 / 7 + frameSize.width * 3 / 2),
      Size(3 * frameSize.width, 3 * frameSize.width),
      scale = 1.0,
      zIndex = 0,
      rotation = 0
    )
    val cardTop = cards.map(_.top).minOption.getOrElse(frameSize.height * 5 / 6)
    PlayerLayout(profile, MessageBox.Center(frameSize.height - cardTop + yOffset / 2), GroupLayout(cards))

  private enum Origin:
    case Left, BottomLeft, TopLeft, Right, TopRight, BottomRight
    def isRight = this match
      case Right | TopRight | BottomRight => true
      case _ => false
    def isBottom = this match
      case BottomLeft | BottomRight => true
      case _ => false
    
  private def otherPlayerOrigin(index: Int, total: Int): Origin =
    (index, total) match
      case (0, 1 | 2 | 3) => Origin.Left
      case (0, 4) => Origin.BottomLeft
      case (1, 2) => Origin.Right
      case (1, 3) => Origin.TopRight
      case (1, 4) => Origin.TopLeft
      case (2, 3) => Origin.BottomRight
      case (2, 4) => Origin.TopRight
      case (3, 4) => Origin.BottomRight

  private def otherPlayerLayout(player: Player, handSize: Int, origin: Origin, frameSize: Size, params: Params) =
    import params.cardSize
    import params.OtherPlayer.*
    val transformX: Position => Position = if origin.isRight then _.reflectX(frameSize.width / 2) else identity
    val transformY: Position => Position = origin match
      case Origin.Left | Origin.Right => _.translate(yOffset = frameSize.height / 6 + profileSize.height / 2)
      case Origin.BottomLeft | Origin.BottomRight => _.translate(yOffset = frameSize.height / 3  + profileSize.height / 2)
      case _ => _.translate(yOffset = profileSize.height / 2)
    val transformXAndY = transformX.andThen(transformY)
    val cardRowSize = origin match
      case Origin.Left | Origin.Right => 8
      case _ => 6
    val relativePositions = 0.until(handSize).map: i =>
      val row = i / cardRowSize
      val column = i % cardRowSize
      val yOffset = if row % 2 == 1 then (cardVerticalSpacing / 2) else 0
      Position(row * cardHorizontalSpacing, column * cardVerticalSpacing + 3 *  profileSize.height / 4 - yOffset)
    val minX = relativePositions.map(_.x).minOption.getOrElse(0)
    val translateX = frameSize.width / 12 - minX / 2
    val rotation = if origin.isRight then -90 else 90
    val cards = relativePositions.map(_.translate(xOffset = translateX)).zipWithIndex.map:
      case (pos, i) => ElementLayout(s"player-${player.id}-card-$i", transformXAndY(pos), cardSize, cardScale, zIndex = i + 10, rotation)
    val profilePos = transformXAndY(Position(profileSize.width, 0))
    val profile = ElementLayout(s"player-${player.id}-profile", profilePos, profileSize, scale = 1.0, zIndex = 0, rotation = 0)
    val messageBox =
      if origin.isRight then MessageBox.Right(5 * profileSize.width / 4, profilePos.y + profileSize.height / 4)
      else MessageBox.Left(5 * profileSize.width / 4, profilePos.y + profileSize.height / 4)
    PlayerLayout(profile, messageBox, GroupLayout(cards))
