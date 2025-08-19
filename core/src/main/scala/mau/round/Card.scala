package mau.round

import io.circe.Codec

enum Suit derives Codec:
  case Club, Heart, Spade, Diamond
  def id: String = toString.toLowerCase
  def isBlack: Boolean = this match
    case Club | Spade => true
    case _ => false
  def isRed: Boolean = !isBlack
  def isClub: Boolean = this == Club
  def isDiamond: Boolean = this == Diamond
  def isHeart: Boolean = this == Heart
  def isSpade: Boolean = this == Spade
  
object Suit:
  given Ordering[Suit] = Ordering.by(_.ordinal)

enum Rank derives Codec:
  case Ace, Two, Three, Four, Five, Six, Seven, Eight, Nine, Ten, Jack, Queen, King
  def value: Int = ordinal + 1
  def id: String = toString.toLowerCase
  def isEven: Boolean = !isFace && value % 2 == 0
  def isOdd: Boolean = !isFace && value % 2 == 1
  def isFace: Boolean = this match
    case Jack | Queen | King => true
    case _ => false
  
object Rank:
  given Ordering[Rank] = Ordering.by(_.ordinal)

final case class Card(rank: Rank, suit: Suit) derives Codec:
  def id: String = s"${rank.id}-of-${suit.id}s"
  def isFace: Boolean = rank.isFace
  def isBlack: Boolean = suit.isBlack
  def isRed: Boolean = suit.isRed
  def isClub: Boolean = suit.isClub
  def isDiamond: Boolean = suit.isDiamond
  def isHeart: Boolean = suit.isHeart
  def isSpade: Boolean = suit.isSpade
  def isEven: Boolean = rank.isEven
  def isOdd: Boolean = rank.isOdd

object Card:
  given Ordering[Card] = Ordering.by(c => (c.suit, c.rank))
  val allCards: Seq[Card] =
    for rank <- Rank.values.toSeq; suit <- Suit.values
    yield Card(rank, suit)
