package com.example.myfirstweather.ui

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myfirstweather.data.MoonPhaseUtils
import com.example.myfirstweather.data.WeatherCodeUtils
import com.example.myfirstweather.data.WeatherUIModel
import com.patrykandpatrick.vico.compose.axis.axisLabelComponent
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.component.shape.shader.verticalGradient
import com.patrykandpatrick.vico.core.chart.line.LineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Theme Colors for better readability
private val CardBackground = Color(0x33000000) // Darker semi-transparent black
private val ContentColor = Color.White
private val SecondaryContentColor = Color.White.copy(alpha = 0.7f)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(viewModel: WeatherViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val locationName by viewModel.currentLocationName.collectAsState()
    val suggestions by viewModel.searchSuggestions.collectAsState()
    
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A237E), // Deep Night Blue
                        Color(0xFF121212)  // Near Black
                    )
                )
            )
    ) {
        when (val state = uiState) {
            is WeatherUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ContentColor)
                }
            }
            is WeatherUiState.Success -> {
                WeatherContent(locationName, state.forecast)
            }
            is WeatherUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.message, color = ContentColor, textAlign = TextAlign.Center)
                }
            }
        }

        // Search UI Overlay
        if (isSearchActive) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)).padding(24.dp)) {
                Column {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { 
                            searchQuery = it
                            viewModel.onSearchQueryChanged(it)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search city...") },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black
                        )
                    )
                    if (suggestions.isNotEmpty()) {
                        Card(
                            modifier = Modifier.padding(top = 8.dp).fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column {
                                suggestions.forEach { result ->
                                    ListItem(
                                        headlineContent = { Text(result.name, color = Color.Black) },
                                        supportingContent = { Text("${result.admin1 ?: ""}, ${result.country}", color = Color.Gray) },
                                        modifier = Modifier.clickable {
                                            viewModel.selectLocation(result)
                                            isSearchActive = false
                                            searchQuery = ""
                                        }
                                    )
                                }
                            }
                        }
                    }
                    TextButton(onClick = { isSearchActive = false }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("CANCEL", color = ContentColor)
                    }
                }
            }
        } else {
            IconButton(onClick = { isSearchActive = true }, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = ContentColor)
            }
        }
    }
}

@Composable
fun WeatherContent(city: String, forecast: List<WeatherUIModel>) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val day = forecast.getOrNull(selectedIndex) ?: forecast.firstOrNull() ?: return

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 60.dp, bottom = 40.dp, start = 20.dp, end = 20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { HeroSection(city, day) }
        
        item {
            SectionTitle("HOURLY FORECAST")
            HourlyRow(day)
        }

        item {
            SectionTitle("10-DAY FORECAST")
            DailyHorizontalRow(forecast, selectedIndex) { selectedIndex = it }
        }

        item {
            SectionTitle("TEMPERATURE TREND")
            TempChart(day.hourlyTemp)
        }

        item {
            SectionTitle("WIND TREND (km/h)")
            WindChart(day.hourlyWind)
        }

        item {
            SectionTitle("RAIN TREND (mm)")
            RainChart(day.hourlyRain)
        }

        if (day.hourlyTide.isNotEmpty()) {
            item {
                SectionTitle("TIDE HEIGHT TREND (m)")
                MarineChart(day.hourlyTide)
            }
            item {
                TidesSummarySection(day)
            }
        }

        if (day.hourlyPollen.isNotEmpty()) {
            item {
                SectionTitle("POLLEN TREND")
                PollenChart(day.hourlyPollen)
            }
            item {
                PollenSummarySection(day)
            }
        }

        item {
            SectionTitle("DETAILS")
            DetailsGrid(day)
        }

        item {
            MoonSection(day)
        }
    }
}

@Composable
fun HeroSection(city: String, day: WeatherUIModel) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text(city, fontSize = 32.sp, fontWeight = FontWeight.Black, color = ContentColor)
        Spacer(modifier = Modifier.height(8.dp))
        Icon(
            painter = WeatherCodeUtils.getWeatherIcon(day.weatherCode, true),
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = ContentColor
        )
        Text("${day.tempMax.toInt()}°", fontSize = 80.sp, fontWeight = FontWeight.Black, color = ContentColor)
        Text(WeatherCodeUtils.getWeatherDescription(day.weatherCode).uppercase(), fontSize = 18.sp, color = SecondaryContentColor, letterSpacing = 2.sp)
        
        Spacer(modifier = Modifier.height(16.dp))
        FunTipCard(day)
    }
}

@Composable
fun FunTipCard(day: WeatherUIModel) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = getWeatherTip(day.weatherCode, day.tempMax.toInt()),
            modifier = Modifier.padding(16.dp),
            color = ContentColor,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = SecondaryContentColor,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
fun HourlyRow(day: WeatherUIModel) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        itemsIndexed(day.hourlyTemp) { index, temp ->
            val hour = day.hourlyHours.getOrNull(index) ?: 0
            val code = day.hourlyWeatherCode.getOrNull(index) ?: 0
            val isDay = day.hourlyIsDay.getOrNull(index) ?: true
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.background(CardBackground, RoundedCornerShape(16.dp)).padding(12.dp)
            ) {
                Text(
                    if (hour == 0) "12am" else if (hour == 12) "12pm" else if (hour < 12) "${hour}am" else "${hour - 12}pm",
                    color = ContentColor, fontSize = 12.sp
                )
                Icon(
                    painter = WeatherCodeUtils.getWeatherIcon(code, isDay),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).padding(vertical = 4.dp),
                    tint = ContentColor
                )
                Text("${temp.toInt()}°", color = ContentColor, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DailyHorizontalRow(forecast: List<WeatherUIModel>, selectedIndex: Int, onSelect: (Int) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(forecast) { index, day ->
            val isSelected = index == selectedIndex
            Column(
                modifier = Modifier
                    .width(100.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Color.White.copy(alpha = 0.3f) else CardBackground)
                    .clickable { onSelect(index) }
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(if(index == 0) "TODAY" else day.date.take(3).uppercase(), color = ContentColor, fontWeight = FontWeight.Bold)
                Icon(
                    painter = WeatherCodeUtils.getWeatherIcon(day.weatherCode, true),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp).padding(vertical = 4.dp),
                    tint = ContentColor
                )
                Row {
                    Text("${day.tempMax.toInt()}°", color = ContentColor, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${day.tempMin.toInt()}°", color = SecondaryContentColor)
                }
            }
        }
    }
}

@Composable
fun TempChart(temps: List<Double>) {
    if (temps.isEmpty()) return
    val model = entryModelOf(*temps.map { it.toFloat() }.toTypedArray())
    
    Chart(
        chart = lineChart(
            lines = listOf(
                LineChart.LineSpec(
                    lineColor = ContentColor.toArgb(),
                    lineBackgroundShader = verticalGradient(
                        arrayOf(ContentColor.copy(alpha = 0.5f), Color.Transparent)
                    )
                )
            )
        ),
        model = model,
        startAxis = rememberStartAxis(
            label = axisLabelComponent(color = ContentColor),
            guideline = null
        ),
        bottomAxis = rememberBottomAxis(
            label = axisLabelComponent(color = ContentColor),
            guideline = null
        ),
        modifier = Modifier.height(150.dp).fillMaxWidth()
    )
}

@Composable
fun WindChart(winds: List<Double>) {
    if (winds.isEmpty()) return
    val model = entryModelOf(*winds.map { it.toFloat() }.toTypedArray())
    
    Chart(
        chart = lineChart(
            lines = listOf(
                LineChart.LineSpec(
                    lineColor = ContentColor.copy(alpha = 0.8f).toArgb(),
                    lineBackgroundShader = verticalGradient(
                        arrayOf(ContentColor.copy(alpha = 0.2f), Color.Transparent)
                    )
                )
            )
        ),
        model = model,
        startAxis = rememberStartAxis(
            label = axisLabelComponent(color = ContentColor),
            guideline = null
        ),
        bottomAxis = rememberBottomAxis(
            label = axisLabelComponent(color = ContentColor),
            guideline = null
        ),
        modifier = Modifier.height(150.dp).fillMaxWidth()
    )
}

@Composable
fun RainChart(rains: List<Double>) {
    if (rains.isEmpty()) return
    val model = entryModelOf(*rains.map { it.toFloat() }.toTypedArray())
    
    Chart(
        chart = lineChart(
            lines = listOf(
                LineChart.LineSpec(
                    lineColor = Color.Cyan.toArgb(),
                    lineBackgroundShader = verticalGradient(
                        arrayOf(Color.Cyan.copy(alpha = 0.3f), Color.Transparent)
                    )
                )
            )
        ),
        model = model,
        startAxis = rememberStartAxis(
            label = axisLabelComponent(color = ContentColor),
            guideline = null
        ),
        bottomAxis = rememberBottomAxis(
            label = axisLabelComponent(color = Color.White),
            guideline = null
        ),
        modifier = Modifier.height(150.dp).fillMaxWidth()
    )
}

@Composable
fun MarineChart(tides: List<Double>) {
    if (tides.isEmpty()) return
    val model = entryModelOf(*tides.map { it.toFloat() }.toTypedArray())
    
    Chart(
        chart = lineChart(
            lines = listOf(
                LineChart.LineSpec(
                    lineColor = Color(0xFF2196F3).toArgb(),
                    lineBackgroundShader = verticalGradient(
                        arrayOf(Color(0xFF2196F3).copy(alpha = 0.3f), Color.Transparent)
                    )
                )
            )
        ),
        model = model,
        startAxis = rememberStartAxis(
            label = axisLabelComponent(color = ContentColor),
            guideline = null
        ),
        bottomAxis = rememberBottomAxis(
            label = axisLabelComponent(color = ContentColor),
            guideline = null
        ),
        modifier = Modifier.height(150.dp).fillMaxWidth()
    )
}

@Composable
fun PollenChart(pollen: List<Double>) {
    if (pollen.isEmpty()) return
    val model = entryModelOf(*pollen.map { it.toFloat() }.toTypedArray())
    
    Chart(
        chart = lineChart(
            lines = listOf(
                LineChart.LineSpec(
                    lineColor = Color(0xFF8BC34A).toArgb(),
                    lineBackgroundShader = verticalGradient(
                        arrayOf(Color(0xFF8BC34A).copy(alpha = 0.3f), Color.Transparent)
                    )
                )
            )
        ),
        model = model,
        startAxis = rememberStartAxis(
            label = axisLabelComponent(color = ContentColor),
            guideline = null
        ),
        bottomAxis = rememberBottomAxis(
            label = axisLabelComponent(color = ContentColor),
            guideline = null
        ),
        modifier = Modifier.height(150.dp).fillMaxWidth()
    )
}

@Composable
fun DetailsGrid(day: WeatherUIModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DetailCard("WIND", "${day.hourlyWind.firstOrNull()?.toInt() ?: 0} km/h", "km/h", Modifier.weight(1f))
            DetailCard("UV INDEX", "${day.dailyUVMax.toInt()}", getUVDescription(day.dailyUVMax), Modifier.weight(1f), getUVColor(day.dailyUVMax))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            DetailCard("RAIN", "${day.hourlyRain.sum().toInt()} mm", "total today", Modifier.weight(1f))
            DetailCard("HUMIDITY", "${day.hourlyHumidity.firstOrNull()?.toInt() ?: 0}%", "dew point ${day.tempMin.toInt()}°", Modifier.weight(1f))
        }
    }
}

@Composable
fun DetailCard(title: String, value: String, sub: String, modifier: Modifier, valueColor: Color = ContentColor) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = SecondaryContentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(value, color = valueColor, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(sub, color = SecondaryContentColor, fontSize = 12.sp)
        }
    }
}

@SuppressLint("DefaultLocale")
@Composable
fun TidesSummarySection(day: WeatherUIModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("TIDE TIMES", color = SecondaryContentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (day.dailyHighTide != null && day.dailyLowTide != null) {
                    TideSummaryItem("High Tide", String.format(Locale.US, "%.1fm", day.dailyHighTide))
                    TideSummaryItem("Low Tide", String.format(Locale.US, "%.1fm", day.dailyLowTide))
                } else {
                    Text("Tide data unavailable", color = ContentColor, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun TideSummaryItem(label: String, value: String) {
    Column {
        Text(label, color = SecondaryContentColor, fontSize = 14.sp)
        Text(value, color = ContentColor, fontSize = 20.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
fun PollenSummarySection(day: WeatherUIModel) {
    val currentPollen = day.hourlyPollen.firstOrNull() ?: 0.0
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("POLLEN COUNT", color = SecondaryContentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = getPollenDescription(currentPollen),
                color = getPollenColor(currentPollen),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Text("Current level based on regional data", color = SecondaryContentColor, fontSize = 12.sp)
        }
    }
}

fun getPollenDescription(count: Double): String = when {
    count < 1 -> "Negligible"
    count < 10 -> "Low"
    count < 50 -> "Moderate"
    count < 100 -> "High"
    else -> "Very High"
}

fun getPollenColor(count: Double): Color = when {
    count < 10 -> Color(0xFF8BC34A) // Green
    count < 50 -> Color(0xFFFFEB3B) // Yellow
    count < 100 -> Color(0xFFFF9800) // Orange
    else -> Color(0xFFF44336) // Red
}

@Composable
fun MoonSection(day: WeatherUIModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("LUNAR CYCLE", color = SecondaryContentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Start))
            Spacer(modifier = Modifier.height(24.dp))
            MoonCycleChart(day.moonPhase, modifier = Modifier.size(160.dp))
            Spacer(modifier = Modifier.height(24.dp))
            Text(MoonPhaseUtils.getPhaseName(day.moonPhase).uppercase(), color = ContentColor, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            Text("${(MoonPhaseUtils.getIllumination(day.moonPhase) * 100).toInt()}% Illumination", color = SecondaryContentColor, fontSize = 12.sp)
        }
    }
}

@Composable
fun MoonCycleChart(phase: Double, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2.5f
            
            // Draw the background cycle path
            drawCircle(
                color = ContentColor.copy(alpha = 0.1f),
                radius = radius,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )
            
            // Draw phase points (New, First Q, Full, Last Q)
            val majorPhases = listOf(0.0, 0.25, 0.5, 0.75)
            majorPhases.forEach { p ->
                val angle = (p * 2 * PI - PI / 2).toFloat()
                val dotOffset = Offset(
                    center.x + radius * cos(angle),
                    center.y + radius * sin(angle)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.4f),
                    radius = 4.dp.toPx(),
                    center = dotOffset
                )
            }
            
            // Draw current phase indicator on the circle
            val currentAngle = (phase * 2 * PI - PI / 2).toFloat()
            val indicatorOffset = Offset(
                center.x + radius * cos(currentAngle),
                center.y + radius * sin(currentAngle)
            )
            
            // Glow effect
            drawCircle(
                color = Color.White.copy(alpha = 0.3f),
                radius = 10.dp.toPx(),
                center = indicatorOffset
            )
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx(),
                center = indicatorOffset
            )
        }
        
        // Large central moon icon
        MoonPhaseView(phase, modifier = Modifier.size(70.dp))
    }
}

@Composable
fun MoonPhaseView(phase: Double, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2
        val center = Offset(size.width / 2, size.height / 2)
        // Background - Dark part of the moon
        drawCircle(color = Color.White.copy(alpha = 0.2f), radius = radius, center = center)
        
        // Visible illumination logic
        val illumination = MoonPhaseUtils.getIllumination(phase).toFloat()
        if (illumination > 0) {
            drawCircle(color = Color.White, radius = radius * (illumination.coerceIn(0.2f, 1f)), center = center)
        }
    }
}

fun getUVDescription(uv: Double): String = when {
    uv < 3 -> "Low"
    uv < 6 -> "Moderate"
    uv < 8 -> "High"
    else -> "Very High"
}

fun getUVColor(uv: Double): Color = when {
    uv < 3 -> Color(0xFF4CAF50) // Green
    uv < 6 -> Color(0xFFFFEB3B) // Yellow
    uv < 8 -> Color(0xFFFF9800) // Orange
    else -> Color(0xFFF44336)   // Red
}

fun getWeatherTip(code: Int, temp: Int): String {
    return when {
        code == 0 && temp > 25 -> "Tip: Wear sunglasses today! 😎"
        code in 61..67 || code in 80..82 -> "Tip: Don't forget your umbrella! ☔"
        temp < 10 -> "Tip: Bundle up, it's chilly! 🧣"
        else -> "Tip: Have a great day! ✨"
    }
}
