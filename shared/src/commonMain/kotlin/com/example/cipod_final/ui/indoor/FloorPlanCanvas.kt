package com.example.cipod_final.ui.indoor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cipod_final.indoor.FloorPlan
import com.example.cipod_final.indoor.FloorPlanContent
import com.example.cipod_final.indoor.PlanPoint
import com.example.cipod_final.indoor.PlanTransform
import com.example.cipod_final.indoor.RoomMarker
import com.example.cipod_final.ui.theme.ChannelBlue
import com.example.cipod_final.ui.theme.Seafoam
import com.example.cipod_final.ui.theme.WarmCream
import com.example.cipod_final.ui.theme.SunsetOrange
import org.jetbrains.compose.resources.imageResource
import kotlin.math.roundToInt

@Composable
internal fun FloorPlanCanvas(
    plan: FloorPlan,
    markers: List<RoomMarker>,
    editing: Boolean,
    selectedMarkerId: String?,
    onSelectMarker: (String) -> Unit,
    onPlaceMarker: (PlanPoint) -> Unit,
    onMoveMarker: (String, PlanPoint) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMarkers by rememberUpdatedState(markers)
    val selectMarker by rememberUpdatedState(onSelectMarker)
    val placeMarker by rememberUpdatedState(onPlaceMarker)
    val moveMarker by rememberUpdatedState(onMoveMarker)
    val hitRadius = with(LocalDensity.current) { 24.dp.toPx() }
    var transform by remember(plan.id) { mutableStateOf(PlanTransform(aspectRatio = plan.aspectRatio)) }
    val image = (plan.content as? FloorPlanContent.ApprovedImage)?.let { imageResource(it.resource) }
    val measurer = rememberTextMeasurer()
    Column(modifier) {
        Row {
            TextButton(onClick = {
                transform = transform.gesture(1.35f, 0f, 0f, transform.viewportWidth / 2, transform.viewportHeight / 2)
            }) { Text("Zoom +") }
            TextButton(onClick = {
                transform = transform.gesture(1 / 1.35f, 0f, 0f, transform.viewportWidth / 2, transform.viewportHeight / 2)
            }) { Text("Zoom −") }
            TextButton(onClick = { transform = transform.copy(zoom = 1f, panX = 0f, panY = 0f) }) { Text("Reset View") }
        }
        Canvas(Modifier.fillMaxSize().padding(12.dp).clipToBounds().background(Seafoam)
            .onSizeChanged { transform = transform.resized(it.width.toFloat(), it.height.toFloat()) }
            .semantics { contentDescription = "Floor plan. Pinch to zoom or use zoom buttons. ${markers.size} markers; use the Markers list for text access." }
            .pointerInput(plan.id, editing, hitRadius) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val hit = currentMarkers.asReversed().minByOrNull { marker ->
                        val (x, y) = transform.screen(marker.position)
                        (Offset(x, y) - down.position).getDistanceSquared()
                    }?.takeIf { marker ->
                        val (x, y) = transform.screen(marker.position)
                        (Offset(x, y) - down.position).getDistance() <= hitRadius
                    }
                    var moved = false
                    var multiTouch = false
                    var accumulated = Offset.Zero
                    var dragPoint = hit?.position
                    do {
                        val event = awaitPointerEvent()
                        val count = event.changes.count { it.pressed }
                        val pan = event.calculatePan()
                        if (count > 1) {
                            multiTouch = true
                            moved = true
                            val centroid = event.calculateCentroid(useCurrent = false)
                            transform = transform.gesture(event.calculateZoom(), pan.x, pan.y, centroid.x, centroid.y)
                        } else if (count == 1 && !multiTouch) {
                            accumulated += pan
                            val wasMoved = moved
                            if (accumulated.getDistance() > viewConfiguration.touchSlop) moved = true
                            if (moved) {
                                val delta = if (wasMoved) pan else accumulated
                                if (editing && hit != null && dragPoint != null) {
                                    dragPoint = transform.drag(dragPoint, delta.x, delta.y)
                                    moveMarker(hit.id, dragPoint)
                                } else {
                                    transform = transform.gesture(1f, delta.x, delta.y, down.position.x, down.position.y)
                                }
                            }
                        }
                        if (moved) event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                    if (!moved && !multiTouch) {
                        if (hit != null) selectMarker(hit.id)
                        else transform.normalized(down.position.x, down.position.y)?.let { placeMarker(it) }
                    } else if (editing && hit != null && !multiTouch) selectMarker(hit.id)
                }
            }) {
            val t = transform
            drawRect(WarmCream, Offset(t.left, t.top), Size(t.width, t.height))
            if (image != null) {
                drawImage(image, dstOffset = IntOffset(t.left.roundToInt(), t.top.roundToInt()),
                    dstSize = IntSize(t.width.roundToInt().coerceAtLeast(1), t.height.roundToInt().coerceAtLeast(1)))
            } else {
                val variant = (plan.content as FloorPlanContent.Demonstration).variant
                val zones = if (variant == "A") listOf(
                    listOf(.08f, .08f, .32f, .32f), listOf(.6f, .08f, .32f, .32f),
                    listOf(.08f, .6f, .32f, .32f), listOf(.6f, .6f, .32f, .32f)
                ) else listOf(listOf(.1f, .1f, .8f, .2f), listOf(.1f, .55f, .35f, .35f), listOf(.55f, .55f, .35f, .35f))
                zones.forEachIndexed { index, zone ->
                    val (x, y) = t.screen(PlanPoint(zone[0], zone[1]))
                    drawRect(Seafoam, Offset(x, y), Size(zone[2] * t.width, zone[3] * t.height))
                    drawRect(ChannelBlue, Offset(x, y), Size(zone[2] * t.width, zone[3] * t.height), style = Stroke(2.dp.toPx()))
                    drawText(measurer, "Demo zone ${index + 1}", topLeft = Offset(x + 6.dp.toPx(), y + 6.dp.toPx()),
                        style = androidx.compose.ui.text.TextStyle(color = ChannelBlue, fontSize = 12.sp))
                }
            }
            drawRect(Color.Gray, Offset(t.left, t.top), Size(t.width, t.height), style = Stroke(2.dp.toPx()))
            markers.forEach { marker ->
                val (x, y) = t.screen(marker.position)
                val center = Offset(x, y)
                if (marker.id == selectedMarkerId) drawCircle(SunsetOrange, 19.dp.toPx(), center)
                drawCircle(ChannelBlue, 14.dp.toPx(), center)
                drawCircle(Color.White, 14.dp.toPx(), center, style = Stroke(2.dp.toPx()))
                val symbol = measurer.measure(marker.type.symbol,
                    androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 13.sp))
                drawText(symbol, topLeft = center - Offset(symbol.size.width / 2f, symbol.size.height / 2f))
                val label = measurer.measure(marker.name,
                    androidx.compose.ui.text.TextStyle(color = ChannelBlue, fontSize = 12.sp))
                val origin = Offset(x + 18.dp.toPx(), y - label.size.height / 2f)
                drawRect(WarmCream, origin, Size(label.size.width.toFloat(), label.size.height.toFloat()))
                drawText(label, topLeft = origin)
            }
        }
    }
}
