/* package mau

import mau.engine.RoundState
import mau.round.*
import mau.round.Rank.*
import mau.round.Suit.*

import scala.collection.SeqMap
import scala.collection.immutable.ListMap
import scala.util.Random

object Values:
  def random = new Random(0)

  val player1 = Player(User("Bob", "avatar-0"), 1)
  val player2 = Player(User("Adpi2", "avatar-0"), 2)
  val player3 = Player(User("Alice", "avatar-0"), 3)

  def threePlayers(
      discard: Seq[Card] = Seq(Card(Three, Diamond)),
      hand1: Hand = Hand(Card(Ace, Club), Card(Five, Club), Card(Two, Diamond), Card(Ace, Spade), Card(Three, Spade)),
      hand2: Hand = Hand(Card(Four, Club), Card(Nine, Club), Card(Four, Diamond), Card(Five, Diamond), Card(Ten, Heart)),
      hand3: Hand = Hand(Card(Six, Club), Card(Seven, Club), Card(Ten, Diamond), Card(Jack, Diamond), Card(King, Spade)),
      currentPlayer: Player = player1
  ): RoundState =
    val usedCards = hand1.cards.toSet ++ hand2.cards ++ hand3.cards ++ discard
    val remainingCards = Card.allCards.filterNot(usedCards.contains)
    val stack = random.shuffle(remainingCards)
    initRound(
      SeqMap(player1 -> hand1, player2 -> hand2, player3 -> hand3),
      stack,
      discard,
      Direction.ClockWise,
      currentPlayer
    )

  def twoPlayers(
      discard: Seq[Card] = Seq(Card(Three, Diamond)),
      hand1: Hand = Hand(Card(Ace, Club), Card(Five, Club), Card(Two, Diamond), Card(Ace, Spade), Card(Three, Spade)),
      hand2: Hand = Hand(Card(Four, Club), Card(Nine, Club), Card(Four, Diamond), Card(Five, Diamond), Card(Ten, Heart)),
      currentPlayer: Player = player1
  ): RoundState =
    val usedCards = hand1.cards.toSet ++ hand2.cards ++ discard
    val remainingCards = Card.allCards.filterNot(usedCards.contains)
    val stack = random.shuffle(remainingCards)
    initRound(
      SeqMap(player1 -> hand1, player2 -> hand2),
      stack,
      discard,
      Direction.ClockWise,
      currentPlayer
    )

  private def initRound(
    hands: SeqMap[Player, Hand],
    stack: Seq[Card],
    discard: Seq[Card],
    direction: Direction,
    currentPlayer: Player
  ): RoundState =
    val players = hands.keys.toSeq
    assert(players.contains(currentPlayer))
    val cards = hands.flatMap((_, h) => h.cards).toSet ++ stack ++ discard
    assert(cards.size == 52)
    RoundState(
      buttons = Seq.empty,
      stack,
      discard,
      hands.keys.toSeq,
      hands.toMap,
      direction,
      MustPlay(currentPlayer),
      None,
      None,
      None
    )
 */