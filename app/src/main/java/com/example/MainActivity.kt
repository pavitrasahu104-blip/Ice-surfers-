package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.FrostRushDatabase
import com.example.data.PlayerRepository
import com.example.ui.FrostRushApp
import com.example.ui.FrostRushViewModel
import com.example.ui.theme.FrostRushTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = FrostRushDatabase.getInstance(applicationContext)
        val repository = PlayerRepository(database.playerProfileDao())

        setContent {
            FrostRushTheme {
                val vm: FrostRushViewModel = viewModel(
                    factory = FrostRushViewModel.Factory(repository)
                )
                FrostRushApp(
                    viewModel = vm,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
