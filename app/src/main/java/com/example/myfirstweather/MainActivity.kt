package com.example.myfirstweather

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myfirstweather.ui.WeatherScreen
import com.example.myfirstweather.ui.WeatherViewModel
import com.example.myfirstweather.ui.theme.MyFirstWeatherTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyFirstWeatherTheme {
                val viewModel: WeatherViewModel = viewModel()
                WeatherScreen(viewModel = viewModel)
            }
        }
    }
}
