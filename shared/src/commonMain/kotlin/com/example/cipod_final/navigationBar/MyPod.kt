package com.example.cipod_final.navigationBar

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cipodfinal.shared.generated.resources.Res
import cipodfinal.shared.generated.resources.placeholder_map
import org.jetbrains.compose.resources.painterResource

@Composable
fun MyPodScreen(modifier: Modifier = Modifier) {
    var search by rememberSaveable { mutableStateOf("") }
    var picked by rememberSaveable { mutableStateOf<String?>(null) }

    // list of buildings (add more later)
    val buildings = listOf("Sierra Hall", "Gateway Hall")

    // check which buildings match the search
    val results = buildings.filter { building ->
        building.contains(search.trim(), ignoreCase = true)
    }

    Column(modifier = modifier.fillMaxSize())
    {
        Text(
            text = "Welcome to MyPod",
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            color = Color.Black,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )

        // the search box
        OutlinedTextField(
            value = search,
            onValueChange = { newText ->
                search = newText
                picked = null
            },
            label = { Text("Search") },
            placeholder = { Text("Example: Sierra Hall") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        // if user picked a building, display it
        if (picked != null) {
            Text("$picked selected.", Modifier.padding(16.dp))
        } else if (search.isNotBlank()) {
            // lists matches
            if (results.isEmpty()) {
                Text("Not found.", Modifier.padding(16.dp))
            } else {
                for (building in results) {
                    TextButton(onClick = {
                        search = building
                        picked = building
                    }) {
                        Text(building)
                    }
                }
            }
        }
        Image(
            painter = painterResource(Res.drawable.placeholder_map),
            contentDescription = "Campus map",
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentScale = ContentScale.FillBounds
        )
    }
}