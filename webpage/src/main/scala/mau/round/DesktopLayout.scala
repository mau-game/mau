package mau.round

import mau.game.StateProjection.View

object DesktopLayout:
  private class Params(scale: Double):
    extension (x: Int)
      private def scaled: Int = (x * scale).toInt

    val cardSize = Size(126.scaled, 180.scaled)
    val discardStackSpacing = 10.scaled
    object MainPlayer:
      val cardHorizontalSpacing = 56.scaled
      val cardVerticalSpacing = 80.scaled
      val yOffset = 40.scaled

    object OtherPlayer:
      val profileSize = Size(66.scaled, 66.scaled)
      val cardScale = 2.0/3
      val cardRowSize = 10

      object Vertical:
        val cardHorizontalSpacing: Int = -37.scaled
        val cardVerticalSpacing: Int = 26.scaled
        val horizontalOffset: Int = -26.scaled

      object Horizontal:
        val cardHorizontalSpacing: Int = 26.scaled
        val cardVerticalSpacing: Int = 37.scaled
        val verticalOffset: Int = -26.scaled
  end Params

  private object Params:
    def apply(frameSize: Size): Params =
      val scale = Math.max(2.0 / 3, Math.min(frameSize.width.toDouble / 1024, frameSize.height.toDouble / 868))
      new Params(scale)

  def apply(you: Player, round: View.Round, frameSize: Size): RoundLayout =
    val params: Params = Params(frameSize)
    import params.*
    val otherPlayers = round.otherPlayers.zipWithIndex.map:
      case ((player, handSize), i) =>
        val origin = otherPlayerOrigin(i, round.otherPlayers.size)
        player -> otherPlayerLayout(player, handSize, origin, frameSize, params)
    val penalty = PenaltyBox(left = frameSize.width / 3, bottom = frameSize.height / 2 + cardSize.height / 2, width = frameSize.width / 3)
    val stackPos = Position((frameSize.width - discardStackSpacing - cardSize.width) / 2 , frameSize.height / 2)
    val discardPos = Position((frameSize.width + discardStackSpacing + cardSize.width) / 2, frameSize.height / 2)
    val stack = ElementLayout("stack", stackPos, cardSize, scale = 1.0, zIndex = 1, rotation = 0)
    val discard = round.discard.zipWithIndex.map: (card, i) =>
      ElementLayout(s"discard-${card.id}", discardPos, cardSize, scale = 1.0, zIndex = round.discard.size - i, rotation = 0)
    val mainPlayer = round.hand.map(mainPlayerLayout(you, _, frameSize, params))
    RoundLayout(otherPlayers.toMap, penalty, stack, discard, mainPlayer)

  private def mainPlayerLayout(you: Player, hand: Hand, frameSize: Size, params: Params): PlayerLayout =
      import params.cardSize
      import params.MainPlayer.*
      val cardRowSize: Int = 10
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
        Position(frameSize.width / 2, frameSize.height * 6 / 7 + frameSize.height),
        Size(2 * frameSize.height, 2 * frameSize.height),
        scale = 1.0,
        zIndex = 0,
        rotation = 0
      )
      val cardTop = cards.map(_.top).minOption.getOrElse(frameSize.height * 5 / 6)
      PlayerLayout(profile, MessageBox.Center(frameSize.height - cardTop + yOffset / 2), GroupLayout(cards))

  private enum Origin:
    case Left, TopLeft, Top, TopRight, Right
    def isRight = this match
      case Right | TopRight => true
      case _ => false
    def isTop = this match
      case TopLeft | TopRight | Top => true
      case _ => false

  private def otherPlayerOrigin(index: Int, total: Int): Origin =
    (index, total) match
      case (0, 1) => Origin.Top
      case (0, 2 | 3 | 4) => Origin.Left
      case (1, 2) => Origin.Right
      case (1, 3) => Origin.Top
      case (1, 4) => Origin.TopLeft
      case (2, 3) => Origin.Right
      case (2, 4) => Origin.TopRight
      case (3, 4) => Origin.Right

  private def otherPlayerLayout(player: Player, handSize: Int, origin: Origin, frameSize: Size, params: Params) =
    origin match
      case Origin.Left | Origin.Right => verticalLayout(player, handSize, origin, frameSize, params)
      case _ => horizontalLayout(player, handSize, origin, frameSize, params)

  private def verticalLayout(player: Player, handSize: Int, origin: Origin, frameSize: Size, params: Params) =
    import params.cardSize
    import params.OtherPlayer.*
    import params.OtherPlayer.Vertical.*
    val transformX: Position => Position =
      if origin.isRight then _.translate(xOffset = frameSize.width / 16).reflectX(frameSize.width / 2)
      else _.translate(xOffset = frameSize.width / 16)
    val transformXAndY = transformX.andThen(_.translate(yOffset = frameSize.height / 4 + profileSize.height / 2))
    val relativePositions = 0.until(handSize).map: i =>
      val row = i / cardRowSize
      val column = i % cardRowSize
      val yOffset = if row % 2 == 1 then (cardVerticalSpacing / 2) else 0
      Position(row * cardHorizontalSpacing, column * cardVerticalSpacing + 2 * profileSize.height - yOffset)
    val minX = relativePositions.map(_.x).minOption.getOrElse(0)
    val rotation = if origin.isRight then -90 else 90
    val cards = relativePositions.map(_.translate(xOffset = - minX / 2)).zipWithIndex.map:
      case (pos, i) => ElementLayout(s"player-${player.id}-card-$i", transformXAndY(pos), cardSize, cardScale, zIndex = i + 10, rotation)
    val profilePos = transformXAndY(Position(0, profileSize.height / 2))
    val profile = ElementLayout(s"player-${player.id}-profile", profilePos, profileSize, scale = 1.0, zIndex = 0, rotation = 0)
    val messageBox =
      if origin.isRight then MessageBox.Right(frameSize.width / 16 + profileSize.width / 4, profilePos.y + profileSize.height / 4)
      else MessageBox.Left(frameSize.width / 16 + profileSize.width / 4, profilePos.y + profileSize.height / 4)
    PlayerLayout(profile, messageBox, GroupLayout(cards))

  private def horizontalLayout(player: Player, handSize: Int, origin: Origin, frameSize: Size, params: Params) =
    import params.cardSize
    import params.OtherPlayer.*
    import params.OtherPlayer.Horizontal.*
    val transformXAndY: Position => Position = origin match
      case Origin.TopRight => _.translate(5 * frameSize.width / 8 + profileSize.width, frameSize.height / 8)
      case Origin.Top => _.translate(xOffset = frameSize.width / 3 + profileSize.width, frameSize.height / 8)
      case _ => _.translate(frameSize.width / 8 + profileSize.width / 2, frameSize.height / 8)
    val relativePositions = 0.until(handSize).map: i =>
      val row = i / cardRowSize
      val column = i % cardRowSize
      val xOffset = if row % 2 == 1 then (cardHorizontalSpacing / 2) else 0
      Position(column * cardHorizontalSpacing + 3 * profileSize.width / 2 - xOffset, row * cardVerticalSpacing)
    val maxY = relativePositions.map(_.y).maxOption.getOrElse(0)
    val cards = relativePositions.map(_.translate(yOffset = - maxY / 2)).zipWithIndex.map:
      case (pos, i) => ElementLayout(s"player-${player.id}-card-$i", transformXAndY(pos), cardSize, cardScale, zIndex = i + 10, rotation = 0)
    val profilePos = transformXAndY(Position(0, 0))
    val profile = ElementLayout(s"player-${player.id}-profile", profilePos, profileSize, scale = 1.0, zIndex = 0, rotation = 0)
    val messageBox = MessageBox.Left(profilePos.x + profileSize.width / 4, profilePos.y + profileSize.width / 4)
    PlayerLayout(profile, messageBox, GroupLayout(cards))
