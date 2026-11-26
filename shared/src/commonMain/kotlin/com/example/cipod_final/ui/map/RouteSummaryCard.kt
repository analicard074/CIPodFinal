package com.example.cipod_final.ui.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cipod_final.routing.WalkingRouteDetails

// Compact route sheet content; callers can later host it in an expandable bottom sheet
@Composable
internal fun RouteSummaryCard(
    route: WalkingRouteDetails,
    onEndRoute: () -> Unit,
    modifier: Modifier = Modifier,
    contentWindowInsets: WindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom),
) {
    val teal = Color(0xFF244A5C)
    Surface(
        modifier = modifier.fillMaxWidth(.94f),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        color = Color(0xFFFDF8F0),
        shadowElevation = 4.dp,
    ) {
        Column(
            Modifier.windowInsetsPadding(contentWindowInsets)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    route.durationLabel, modifier = Modifier.alignByBaseline(),
                    color = teal, fontSize = 24.sp, fontWeight = FontWeight.Medium
                )
                Text(
                    route.distanceLabel, modifier = Modifier.alignByBaseline(),
                    color = teal.copy(alpha = .65f), fontSize = 15.sp
                )
            }
            Text(
                route.destinationName,
                color = teal.copy(alpha = .7f), fontSize = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            OutlinedButton(
                onClick = onEndRoute,
                modifier = Modifier.padding(top = 4.dp).fillMaxWidth(.55f).heightIn(min = 48.dp),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.5.dp, teal),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = teal),
            ) {
                Text("End route", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
