package mau.round

import mau.game.StateProjection.View
import org.scalajs.dom.DOMRect

import scala.collection.SeqMap

case class RoundLayout(
  otherPlayers: Map[Player, PlayerLayout],
  penalty: PenaltyBox,
  stack: ElementLayout,
  discard: Seq[ElementLayout],
  mainPlayer: Option[PlayerLayout]
):
  def lastCard(player: Player): (String, ElementLayout) = otherPlayers(player).hand.elems.last
  def hand: Option[GroupLayout] = mainPlayer.map(_.hand)
  
  val allCards: Map[String, ElementLayout] = 
    otherPlayers.values.flatMap(_.cards).toMap
      ++ Seq(stack.id -> stack)
      ++ discard.map(card => card.id -> card)
      ++ hand.toSeq.flatMap(_.elems)
  
  val allElements: Map[String, ElementLayout] =
    otherPlayers.values.flatMap(_.allElements).toMap
    ++ Seq(stack.id -> stack)
    ++ discard.map(card => card.id -> card)
    ++ mainPlayer.toSeq.flatMap(_.allElements)
end RoundLayout

object RoundLayout:
  def apply(you: Player, round: View.Round, frameSize: Size): RoundLayout =
    if frameSize.isLandscape then DesktopLayout(you, round, frameSize)
    else MobileLayout(you, round, frameSize)

case class Position(x: Int, y: Int):
  def translate(xOffset: Int = 0, yOffset: Int = 0): Position = Position(x + xOffset, y + yOffset)
  def reflectX(xAxis: Int): Position = Position(2 * xAxis - x, y)

case class Size(width: Int, height: Int):
  def isLandscape: Boolean = width > height

object Size:
  def apply(rect: DOMRect): Size = Size(rect.width.toInt, rect.height.toInt)

case class ElementLayout(id: String, center: Position, size: Size, scale: Double, zIndex: Int, rotation: Int):
  export size.*
  export center.*
  def left = center.x - size.width / 2
  def top = center.y - size.height / 2

object ElementLayout:
  def empty(id: String): ElementLayout =
    ElementLayout(id, Position(0, 0), Size(0, 0), scale = 1.0, zIndex = 0, rotation = 0)

case class PenaltyBox(left: Int, bottom: Int, width: Int)

enum MessageBox:
  case Left(left: Int, top: Int)
  case Right(right: Int, top: Int)
  case Center(bottom: Int)

object MessageBox:
  def empty = Left(0, 0)

case class GroupLayout(elems: SeqMap[String, ElementLayout]):
  def apply(id: String): ElementLayout = elems(id)

object GroupLayout:
  def apply(elems: Seq[ElementLayout]): GroupLayout =
    GroupLayout(SeqMap(elems.map(e => e.id -> e)*))

case class PlayerLayout(profile: ElementLayout, messageBox: MessageBox, hand: GroupLayout):
  val cards = hand.elems
  val allElements = hand.elems + (profile.id -> profile)
