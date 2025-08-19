package mau.round

import scala.annotation.targetName
import io.circe.Codec

final case class Hand(cards: Seq[Card]) derives Codec:
  assert(cards.sorted == cards)
  def size: Int = cards.size
  def remove(card: Card): Hand = Hand(cards.filterNot(_ == card))
  def add(others: Card*): Hand = Hand(cards ++ others)
  def contains(card: Card): Boolean = cards.contains(card)
  def isEmpty: Boolean = cards.isEmpty

object Hand:
  def apply(cards: Seq[Card]) = new Hand(cards.sorted)
  @targetName("varargsApply") def apply(cards: Card*): Hand = apply(cards)
