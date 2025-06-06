package com.mateusz.battleshipclient

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView

import com.mateusz.battleshipclient.network.ApiClient
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
    lateinit var btnSub : Button
    lateinit var btnMultiply : Button
    lateinit var btnDivision : Button
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
        btnSub.setOnClickListener(this)
        btnMultiply.setOnClickListener(this)
        btnDivision.setOnClickListener(this)



    }

    override fun onClick(v: View?) {
        // Check Server connection
        lifecycleScope.launch {
            val connected = checkServerConnection()
            if (connected) {
                resultTv.text = "Server OK"
            } else {
                resultTv.text = "Server failed"
                showServerDialog("Connection Error")
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

    private fun showServerDialog(title: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage("Server connection failed.")
            .setPositiveButton("OK", null)
            .show()
    }






}