package com.mateusz.battleshipclient

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

import com.mateusz.battleshipclient.network.ApiClient
import com.mateusz.battleshipclient.model.JoinGameRequest
import com.mateusz.battleshipclient.model.ShipPosition

import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
/*
import com.example.battleships_mateusz_sander.defaultShips
import com.example.battleships_mateusz_sander.lobbyPlayers
import com.example.battleships_mateusz_sander.model.JoinGameRequest
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
 */

class MainActivity : AppCompatActivity() , View.OnClickListener {

    lateinit var btnAdd : Button
    lateinit var etA : EditText
    lateinit var etB : EditText
    lateinit var resultTv : TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnAdd = findViewById(R.id.btn_add)
        etA = findViewById(R.id.et_a)
        etB = findViewById(R.id.et_b)
        resultTv = findViewById(R.id.result_tv)

        btnAdd.setOnClickListener(this)



    }

    override fun onClick(v: View?) {
        lifecycleScope.launch {
            val connected = checkServerConnection()
            if (!connected) {
                resultTv.text = "Server failed"
                showServerDialog("Connection Error")
                return@launch
            } else {
                resultTv.text = "Server OK"
            }

            val userValues = getUserValues()
            if (userValues == null) return@launch
            val (gameKey, playerName) = userValues
            resultTv.text = "GameKey: $gameKey\nPlayer: $playerName"

            // Create JoinGameRequest and send
            val joinReq = JoinGameRequest(
                player = playerName,
                gamekey = gameKey,
                ships = getDefaultShips()
            )

            try {
                val response = ApiClient.api.joinGame(joinReq)
                resultTv.text = "Joined game!\nResponse: $response"
            } catch (e: Exception) {
                resultTv.text = "Join failed: ${e.message}"
            }
        }
    }


    suspend fun checkServerConnection(): Boolean {
        return try {
            ApiClient.api.ping()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getUserValues(): Pair<String, String>? {
        val gameKey = etA.text.toString()
        val playerName = etB.text.toString()
        if (gameKey.length < 3 || playerName.length < 3) {
            showServerDialog("Both Game Key and Player Name must be at least 3 characters long")
            return null
        }
        return Pair(gameKey, playerName)
    }


    private fun showServerDialog(title: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("Server connection failed.")
            .setPositiveButton("OK", null)
            .show()
    }

    fun getDefaultShips(): List<ShipPosition> = listOf(
        ShipPosition("Carrier",    0, 3, "horizontal"),
        ShipPosition("Battleship", 1, 1, "vertical"),
        ShipPosition("Destroyer",  2, 4, "horizontal"),
        ShipPosition("Submarine",  3, 3, "vertical"),
        ShipPosition("PatrolBoat", 5, 5, "horizontal")
    )





}