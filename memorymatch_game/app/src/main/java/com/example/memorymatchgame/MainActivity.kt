package com.example.memorymatchgame

import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.gridlayout.widget.GridLayout
import kotlin.random.Random

// PUBLIC_INTERFACE
class MainActivity : AppCompatActivity() {

    // Color constants based on the provided palette
    private val primaryColor = Color.parseColor("#093662")
    private val secondaryColor = Color.parseColor("#dd0e0e")
    private val accentColor = Color.parseColor("#c09926")
    private val lightCardBg = Color.parseColor("#f4f8fa")
    private val cardBackColor = Color.parseColor("#e6eff8")

    // Game configuration
    private val numRows = 4
    private val numCols = 4
    private val numPairs = (numRows * numCols) / 2

    // State variables
    private lateinit var gridLayout: GridLayout
    private lateinit var moveCounterText: TextView
    private lateinit var restartButton: Button

    private var cardViews = mutableListOf<CardViewHolder>()
    private var cards = mutableListOf<MemoryCard>()
    private var firstFlipped: CardViewHolder? = null
    private var secondFlipped: CardViewHolder? = null
    private var moves = 0
    private var canFlip = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initMainUi()
        startNewGame()
    }

    /**
     * PUBLIC_INTERFACE
     * Sets up the UI programmatically: move counter, restart button, and the card grid.
     */
    private fun initMainUi() {
        // Container layout
        val root = ConstraintLayout(this)
        root.setBackgroundColor(Color.WHITE)

        // Moves TextView
        moveCounterText = TextView(this).apply {
            textSize = 20f
            setTextColor(primaryColor)
            text = "Moves: 0"
            id = View.generateViewId()
        }
        root.addView(moveCounterText)

        // Restart Button
        restartButton = Button(this).apply {
            text = "Restart"
            setBackgroundColor(accentColor)
            setTextColor(Color.WHITE)
            id = View.generateViewId()
            textSize = 18f
            setOnClickListener { startNewGame() }
        }
        root.addView(restartButton)

        // Grid layout for cards
        gridLayout = GridLayout(this).apply {
            rowCount = numRows
            columnCount = numCols
            id = View.generateViewId()
            setPadding(12, 16, 12, 16)
            setBackgroundColor(Color.TRANSPARENT)
            // Will add card views later
        }
        root.addView(gridLayout)

        // Constraint layout setup
        val set = ConstraintSet()
        set.clone(root)

        set.connect(moveCounterText.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 32)
        set.connect(moveCounterText.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 32)

        set.connect(restartButton.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 32)
        set.connect(restartButton.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 32)

        set.connect(gridLayout.id, ConstraintSet.TOP, moveCounterText.id, ConstraintSet.BOTTOM, 32)
        set.connect(gridLayout.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START, 16)
        set.connect(gridLayout.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, 16)
        set.connect(gridLayout.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 24)

        set.applyTo(root)
        setContentView(root)
    }

    /**
     * PUBLIC_INTERFACE
     * Initializes (or restarts) the Memory Match game.
     * Randomizes card pairs and sets the board for a new game.
     */
    private fun startNewGame() {
        moves = 0
        moveCounterText.text = "Moves: $moves"
        canFlip = true
        firstFlipped = null
        secondFlipped = null
        gridLayout.removeAllViews()
        cardViews.clear()

        // Generate pair content (icon codes or simple numbers/letters)
        val cardImages = (1..numPairs).toList() + (1..numPairs).toList()
        val shuffledImages = cardImages.shuffled(Random(System.currentTimeMillis()))

        cards = shuffledImages.mapIndexed { idx, id ->
            MemoryCard(id = idx, pairCode = id)
        }.toMutableList()

        // Add card views to grid
        for (i in 0 until numRows * numCols) {
            val card = cards[i]
            val holder = CardViewHolder(this, card)
            holder.view.setOnClickListener { onCardClick(holder) }
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = 0
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(10, 10, 10, 10)
            }
            holder.view.layoutParams = params
            gridLayout.addView(holder.view)
            cardViews.add(holder)
        }
        // Grid aspect ratio: equal cell sizes
        gridLayout.post {
            val size = (gridLayout.width.coerceAtMost(gridLayout.height)) / numRows
            for (i in 0 until gridLayout.childCount) {
                val cardView = gridLayout.getChildAt(i)
                cardView.layoutParams.width = size
                cardView.layoutParams.height = size
            }
        }
    }

    /**
     * Handles tap on a card, manages flipping, matching, and move tracking.
     */
    private fun onCardClick(holder: CardViewHolder) {
        if (!canFlip || holder.card.isMatched || holder.card.isRevealed || holder == firstFlipped) return

        holder.flipToFront()
        holder.card.isRevealed = true

        if (firstFlipped == null) {
            firstFlipped = holder
        } else if (secondFlipped == null) {
            secondFlipped = holder
            canFlip = false // Block input until match resolved

            moves += 1
            moveCounterText.text = "Moves: $moves"

            // Check match after short delay for visual feedback
            Handler(Looper.getMainLooper()).postDelayed({
                checkForMatch()
            }, 650)
        }
    }

    /**
     * Checks if two revealed cards are a match.
     * If matched, keeps them face up. Otherwise, flips them back.
     */
    private fun checkForMatch() {
        if (firstFlipped != null && secondFlipped != null) {
            val one = firstFlipped!!
            val two = secondFlipped!!
            if (one.card.pairCode == two.card.pairCode) {
                one.card.isMatched = true
                two.card.isMatched = true
                one.shineOnMatch(accentColor)
                two.shineOnMatch(accentColor)
                if (allMatched()) {
                    Toast.makeText(this, "Congratulations! Completed in $moves moves.", Toast.LENGTH_LONG).show()
                }
            } else {
                one.flipToBack()
                two.flipToBack()
                one.card.isRevealed = false
                two.card.isRevealed = false
            }
        }
        firstFlipped = null
        secondFlipped = null
        canFlip = true
    }

    /**
     * Returns true if all cards are matched.
     */
    private fun allMatched(): Boolean {
        return cards.all { it.isMatched }
    }

    /**
     * Data class for a memory card's state.
     */
    data class MemoryCard(
        val id: Int,
        val pairCode: Int,
        var isMatched: Boolean = false,
        var isRevealed: Boolean = false,
    )

    /**
     * CardViewHolder encapsulates the card's UI and flip animation logic.
     */
    class CardViewHolder(
        private val activity: AppCompatActivity,
        val card: MemoryCard
    ) {
        val view: FrameLayout = FrameLayout(activity)
        private val cardFront: TextView
        private val cardBack: TextView

        init {
            // Card front: shows the card's pair code (will use emoji or numbers)
            cardFront = TextView(activity).apply {
                text = emojiForPair(card.pairCode)
                textSize = 32f
                setTextColor(activity.getColor(android.R.color.white))
                setBackgroundColor(activity.getColor(android.R.color.holo_blue_dark))
                gravity = android.view.Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                visibility = View.INVISIBLE
            }
            // Card back (default side): should look generic
            cardBack = TextView(activity).apply {
                text = ""
                textSize = 32f
                setBackgroundColor(Color.parseColor("#e6eff8"))
                gravity = android.view.Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            view.addView(cardFront)
            view.addView(cardBack)

            view.elevation = 8f
        }

        fun flipToFront() {
            // Smooth flip animation
            cardBack.animate().rotationY(90f).setDuration(120).withEndAction {
                cardBack.visibility = View.INVISIBLE
                cardFront.visibility = View.VISIBLE
                cardFront.rotationY = -90f
                cardFront.animate().rotationY(0f).setDuration(120).start()
            }.start()
        }

        fun flipToBack() {
            // Smooth flip animation (reverse)
            cardFront.animate().rotationY(90f).setDuration(120).withEndAction {
                cardFront.visibility = View.INVISIBLE
                cardBack.visibility = View.VISIBLE
                cardBack.rotationY = -90f
                cardBack.animate().rotationY(0f).setDuration(120).start()
            }.start()
        }

        fun shineOnMatch(color: Int) {
            view.setBackgroundColor(color)
            // Pulse animation (optional)
            ObjectAnimator.ofFloat(view, "alpha", 1f, 0.7f, 1f).apply {
                duration = 400
                start()
            }
        }

        companion object {
            private fun emojiForPair(pairCode: Int): String {
                // Simple emoji set for up to 8 pairs
                val emojis = listOf("🍎", "🍋", "🍓", "🍇", "🍪", "🍌", "🍒", "🥝")
                return if (pairCode in 1..emojis.size) emojis[pairCode - 1] else pairCode.toString()
            }
        }
    }
}
