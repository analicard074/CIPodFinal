package com.example.cipod_final.navigationBar

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Welcome to MyPod",
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            color = Color.Black,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
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
