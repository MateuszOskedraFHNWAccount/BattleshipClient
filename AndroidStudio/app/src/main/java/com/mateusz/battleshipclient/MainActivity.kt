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
import androidx.core.view.children

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private lateinit var btnStart: Button
    private lateinit var btnRandomize: Button
    private lateinit var etGameKey: EditText
    private lateinit var etPlayer: EditText
    private lateinit var resultTv: TextView
    private lateinit var ownGrid: GridLayout
    private lateinit var enemyGrid: GridLayout

    /* ---- game state ---- */
    private var gameKey = ""
    private var player = ""
    private var gameOver = false
    private var myTurn = false

    /* ---- ships ---- */
    private val shipDefs = listOf(
        "Carrier" to 5, "Battleship" to 4, "Destroyer" to 3,
        "Submarine" to 3, "PatrolBoat" to 2
    )
    private var currentShips: List<ShipPosition> = emptyList()

    /* ---------- lifecycle ---------- */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStart     = findViewById(R.id.btn_add)
        btnRandomize = findViewById(R.id.btn_randomize)
        etGameKey    = findViewById(R.id.et_a)
        etPlayer     = findViewById(R.id.et_b)
        resultTv     = findViewById(R.id.result_tv)
        ownGrid      = findViewById(R.id.board_grid_own)
        enemyGrid    = findViewById(R.id.board_grid_enemy)

        btnStart.setOnClickListener(this)
        btnRandomize.setOnClickListener { randomizeShips() }

        fillGrid(ownGrid,   "O")
        fillGrid(enemyGrid, "E")
    }

    /* ---------- build a 10×10 grid ---------- */
    private fun fillGrid(grid: GridLayout, prefix: String) {
        grid.removeAllViews()
        val cell = resources.displayMetrics.widthPixels / 10
        repeat(100) { idx ->
            val row = idx / 10
            val col = idx % 10
            val btn = Button(this).apply {
                tag = "$prefix$row$col"
                gravity = Gravity.CENTER
                typeface = Typeface.DEFAULT_BOLD
                setBackgroundColor(Color.WHITE)
                layoutParams = GridLayout.LayoutParams().apply {
                    width = cell; height = cell
                    rowSpec = GridLayout.spec(row)
                    columnSpec = GridLayout.spec(col)
                    setMargins(1, 1, 1, 1)
                }

                if (prefix == "E") {            // enemy board → fire
                    setOnClickListener {
                        if (!myTurn || gameOver) return@setOnClickListener
                        fireAt(col, row, this)
                    }
                } else {                        // own board → toggle for test
                    setOnClickListener {
                        text = when (text) { "" -> "S"; "S" -> "X"; else -> "" }
                        setBackgroundColor(if (text == "") Color.WHITE else Color.LTGRAY)
                    }
                }
            }
            grid.addView(btn)
        }
    }

    /* ---------- randomise ships ---------- */
    private fun randomizeShips() {
        ownGrid.children.forEach { (it as Button).apply { text=""; setBackgroundColor(Color.WHITE) } }

        val occ = Array(10) { BooleanArray(10) }
        val placed = mutableListOf<ShipPosition>()

        fun fits(len: Int, x: Int, y: Int, h: Boolean): Boolean {
            if (h && x + len > 10) return false
            if (!h && y + len > 10) return false
            repeat(len) {
                val cx = if (h) x + it else x
                val cy = if (h) y     else y + it
                if (occ[cx][cy]) return false
            }
            return true
        }
        fun mark(len: Int, x: Int, y: Int, h: Boolean) {
            repeat(len) {
                val cx = if (h) x + it else x
                val cy = if (h) y     else y + it
                occ[cx][cy] = true
                (ownGrid.getChildAt(cy * 10 + cx) as Button).apply {
                    text = "S"; setBackgroundColor(Color.LTGRAY)
                }
            }
        }

        for ((name,len) in shipDefs) {
            while (true) {
                val h = Random.nextBoolean()
                val x = Random.nextInt(10)
                val y = Random.nextInt(10)
                if (fits(len,x,y,h)) {
                    mark(len,x,y,h)
                    placed += ShipPosition(name,x,y,if(h)"horizontal" else "vertical")
                    break
                }
            }
        }
        currentShips = placed
    }

    /* ---------- Start Game ---------- */
    override fun onClick(v: View?) {
        lifecycleScope.launch {
            if (!ping()) { showDialog("Connection","Server not reachable"); return@launch }
            val inp = getInputs() ?: return@launch
            gameKey = inp.first; player = inp.second

            val ships = if (currentShips.isNotEmpty()) currentShips else defaultShips()
            val join  = JoinGameRequest(player, gameKey, ships)

            resultTv.text = "Joining…"
            try {
                val resp = ApiClient.api.joinGame(join)
                if (resp.x != null && resp.y != null) markEnemyShot(resp.x!!, resp.y!!)
                myTurn   = resp.x == null
                gameOver = resp.gameover
                resultTv.text = if (myTurn) "We start – shoot!" else "Waiting for enemy…"
                if (!myTurn) waitEnemyFire()
            } catch (e: Exception) {
                resultTv.text = "Join failed: ${e.message}"
            }
        }
    }

    /* ---------- fire / poll ---------- */
    private fun fireAt(x:Int,y:Int,btn:Button)=lifecycleScope.launch{
        try{
            val res=ApiClient.api.fire(FireRequest(player,gameKey,x,y))
            btn.text=if(res.hit)"X" else "O"
            myTurn=false
            if(res.shipsSunk.isNotEmpty()) resultTv.text="We sunk: ${res.shipsSunk}"
            waitEnemyFire()
        }catch(e:Exception){ resultTv.text="Fire error: ${e.message}"}
    }

    private suspend fun waitEnemyFire(){
        while(!myTurn && !gameOver){
            try{
                val res=ApiClient.api.enemyFire(EnemyFireRequest(player,gameKey))
                if(res.x!=null && res.y!=null) markEnemyShot(res.x!!,res.y!!)
                gameOver=res.gameover
                myTurn=!gameOver
            }catch(e:Exception){
                resultTv.text="Enemy poll error: ${e.message}"
                return
            }
        }
        resultTv.text=if(gameOver)"Game over!" else "Your turn – shoot!"
    }

    private fun markEnemyShot(col:Int,row:Int){
        val idx=row*10+col
        val b=ownGrid.getChildAt(idx) as Button
        runOnUiThread{
            b.text=if(b.text=="S")"X" else "O"
            b.setBackgroundColor(Color.RED)
        }
    }

    /* ---------- helpers ---------- */
    private suspend fun ping() = try{ ApiClient.api.ping(); true }catch(_:Exception){ false }

    private fun getInputs():Pair<String,String>?{
        val k=etGameKey.text.toString().trim()
        val n=etPlayer.text.toString().trim()
        return if(k.length<3||n.length<3){
            showDialog("Input","Game Key and Player must be ≥ 3 chars"); null
        } else Pair(k,n)
    }

    private fun showDialog(t:String,m:String)=AlertDialog.Builder(this)
        .setTitle(t).setMessage(m).setPositiveButton("OK",null).show()

    private fun defaultShips()=listOf(
        ShipPosition("Carrier",0,3,"horizontal"),
        ShipPosition("Battleship",1,1,"vertical"),
        ShipPosition("Destroyer",2,4,"horizontal"),
        ShipPosition("Submarine",3,3,"vertical"),
        ShipPosition("PatrolBoat",5,5,"horizontal")
    )
}
