package com.mateusz.battleshipclient

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.gridlayout.widget.GridLayout
import androidx.lifecycle.lifecycleScope
import com.mateusz.battleshipclient.model.*
import com.mateusz.battleshipclient.network.ApiClient
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var btnStart: Button
    private lateinit var btnRandomize: Button
    private lateinit var etGameKey: EditText
    private lateinit var etPlayer: EditText
    private lateinit var resultTv: TextView
    private lateinit var ownGrid: GridLayout
    private lateinit var enemyGrid: GridLayout

    /* ------------ ship data / state ------------ */
    private val shipDefs = listOf(
        "Carrier" to 5,
        "Battleship" to 4,
        "Destroyer" to 3,
        "Submarine" to 3,
        "PatrolBoat" to 2
    )
    private var currentShips: List<ShipPosition> = emptyList()

    /* ------------ lifecycle ------------ */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // basic UI
        btnStart      = findViewById(R.id.btn_add)
        btnRandomize  = findViewById(R.id.btn_randomize)
        etGameKey     = findViewById(R.id.et_a)
        etPlayer      = findViewById(R.id.et_b)
        resultTv      = findViewById(R.id.result_tv)
        ownGrid       = findViewById(R.id.board_grid_own)
        enemyGrid     = findViewById(R.id.board_grid_enemy)

        btnStart.setOnClickListener(this)
        btnRandomize.setOnClickListener { randomizeShips() }

        // build empty 10×10 boards
        fillGrid(ownGrid,   "O")
        fillGrid(enemyGrid, "E")
    }

    /* ------------ grid factory ------------ */
    private fun fillGrid(grid: GridLayout, prefix: String) {
        grid.removeAllViews()
        val cell = resources.displayMetrics.widthPixels / 10   // square px size
        repeat(100) { idx ->
            val row = idx / 10
            val col = idx % 10
            val btn = Button(this).apply {
                tag  = "$prefix$row$col"
                text = ""
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
                setBackgroundColor(Color.WHITE)
                layoutParams = GridLayout.LayoutParams().also { lp ->
                    lp.width = cell; lp.height = cell
                    lp.rowSpec = GridLayout.spec(row)
                    lp.columnSpec = GridLayout.spec(col)
                    lp.setMargins(1, 1, 1, 1)
                }
                setOnClickListener {
                    // simple toggle for demo
                    text = when (text) { "" -> "S"; "S" -> "X"; else -> "" }
                    setBackgroundColor(if (text == "") Color.WHITE else Color.LTGRAY)
                }
            }
            grid.addView(btn)
        }
    }

    /* ------------ randomizer ------------ */
    private fun randomizeShips() {
        // clear board
        for (i in 0 until ownGrid.childCount) {
            (ownGrid.getChildAt(i) as Button).apply {
                text = ""; setBackgroundColor(Color.WHITE)
            }
        }

        val occupied = Array(10) { BooleanArray(10) }
        val result   = mutableListOf<ShipPosition>()

        fun fits(len: Int, x: Int, y: Int, horiz: Boolean): Boolean {
            if (horiz && x + len > 10) return false
            if (!horiz && y + len > 10) return false
            repeat(len) { i ->
                val cx = if (horiz) x + i else x
                val cy = if (horiz) y else y + i
                if (occupied[cx][cy]) return false
            }
            return true
        }
        fun mark(len: Int, x: Int, y: Int, horiz: Boolean) {
            repeat(len) { i ->
                val cx = if (horiz) x + i else x
                val cy = if (horiz) y else y + i
                occupied[cx][cy] = true
                val btn = ownGrid.getChildAt(cy * 10 + cx) as Button
                btn.text = "S"; btn.setBackgroundColor(Color.LTGRAY)
            }
        }

        for ((name, len) in shipDefs) {
            while (true) {
                val horiz = Random.nextBoolean()
                val x = Random.nextInt(10)
                val y = Random.nextInt(10)
                if (fits(len, x, y, horiz)) {
                    mark(len, x, y, horiz)
                    result += ShipPosition(name, x, y,
                        if (horiz) "horizontal" else "vertical")
                    break
                }
            }
        }
        currentShips = result
    }

    /* ------------ button “Start Game” ------------ */
    override fun onClick(v: View?) {
        lifecycleScope.launch {
            if (!checkServerConnection()) {
                resultTv.text = "Server failed"
                showDialog("Connection Error", "Cannot reach server.")
                return@launch
            }
            val (gameKey, playerName) = getUserValues() ?: return@launch

            val shipsToSend = if (currentShips.isNotEmpty()) currentShips else getDefaultShips()
            val joinReq = JoinGameRequest(playerName, gameKey, shipsToSend)

            resultTv.text = "Joining…"
            try {
                val resp = ApiClient.api.joinGame(joinReq)
                resultTv.text = "Joined!\nEnemy fired? x=${resp.x}, y=${resp.y}, gameover=${resp.gameover}"
            } catch (e: Exception) {
                resultTv.text = "Join failed: ${e.message}"
            }
        }
    }

    /* ------------ helpers ------------ */
    private suspend fun checkServerConnection(): Boolean =
        try { ApiClient.api.ping(); true } catch (_: Exception) { false }

    private fun getUserValues(): Pair<String, String>? {
        val key = etGameKey.text.toString().trim()
        val name = etPlayer.text.toString().trim()
        return if (key.length < 3 || name.length < 3) {
            showDialog("Input Error", "Game Key and Player Name must be ≥ 3 chars.")
            null
        } else Pair(key, name)
    }

    private fun showDialog(title: String, msg: String) =
        AlertDialog.Builder(this).setTitle(title).setMessage(msg)
            .setPositiveButton("OK", null).show()

    private fun getDefaultShips(): List<ShipPosition> = listOf(
        ShipPosition("Carrier", 0, 3, "horizontal"),
        ShipPosition("Battleship", 1, 1, "vertical"),
        ShipPosition("Destroyer", 2, 4, "horizontal"),
        ShipPosition("Submarine", 3, 3, "vertical"),
        ShipPosition("PatrolBoat", 5, 5, "horizontal")
    )
}
