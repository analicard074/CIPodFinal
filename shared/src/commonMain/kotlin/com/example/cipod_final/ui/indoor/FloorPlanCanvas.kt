package com.example.cipod_final.ui.indoor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
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
import com.example.cipod_final.indoor.IndoorRoute
import com.example.cipod_final.indoor.PlanPoint
import com.example.cipod_final.indoor.PlanTransform
import com.example.cipod_final.indoor.RoomHighlight
import com.example.cipod_final.indoor.RoomMarker
import com.example.cipod_final.ui.theme.ChannelBlue
import com.example.cipod_final.ui.theme.Seafoam
import com.example.cipod_final.ui.theme.SunsetOrange
import com.example.cipod_final.ui.theme.WarmCream
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.imageResource
import org.maplibre.compose.overlay.ZoomButtonsDefaults

private val IndoorRouteColor = Color(0xFF244BFF)

@Composable
internal fun FloorPlanCanvas(
    plan: FloorPlan,
    markers: List<RoomMarker>,
    editing: Boolean,
    selectedMarkerId: String?,
    onSelectMarker: (String) -> Unit,
    onPlaceMarker: (PlanPoint) -> Unit,
    onMoveMarker: (String, PlanPoint) -> Unit,
    modifier: Modifier = Modifier,
    roomHighlight: RoomHighlight? = null,
    highlightColor: Color? = null,
    route: IndoorRoute? = null,
) {
    val currentMarkers by rememberUpdatedState(markers)
    val selectMarker by rememberUpdatedState(onSelectMarker)
    val placeMarker by rememberUpdatedState(onPlaceMarker)
    val moveMarker by rememberUpdatedState(onMoveMarker)
    val hitRadius = with(LocalDensity.current) { 24.dp.toPx() }
    var transform by remember(plan.id, plan.revision, plan.aspectRatio) {
        mutableStateOf(PlanTransform(aspectRatio = plan.aspectRatio))
    }
    var viewportInitialized by remember(plan.id, plan.revision, plan.aspectRatio) { mutableStateOf(false) }
    val drawable = when (val content = plan.content) {
        is FloorPlanContent.ApprovedImage -> content.resource
        is FloorPlanContent.ReferenceDraftImage -> content.resource
        is FloorPlanContent.Demonstration -> null
    }
    val image = drawable?.let { imageResource(it) }
    val measurer = rememberTextMeasurer()
    Box(modifier) {
        Canvas(Modifier.fillMaxSize().clipToBounds().background(Seafoam)
            .onSizeChanged { size ->
                if (size.width > 0 && size.height > 0) {
                    val resized = transform.resized(size.width.toFloat(), size.height.toFloat())
                    transform = if (!viewportInitialized) {
                        // Fill the viewport and start closer in, centered on the route's arrival on this floor.
                        val zoom = maxOf(
                            1.6f,
                            resized.viewportWidth / resized.width,
                            resized.viewportHeight / resized.height,
                        ).coerceAtMost(6f)
                        val focus = route?.nodes?.firstOrNull { it.floorPlanId == plan.id }?.position
                            ?: roomHighlight?.let { PlanPoint((it.left + it.right) / 2f, (it.top + it.bottom) / 2f) }
                            ?: PlanPoint(.5f, .5f)
                        val zoomed = resized.copy(zoom = zoom)
                        val (x, y) = zoomed.screen(focus)
                        viewportInitialized = true
                        zoomed.gesture(
                            factor = 1f,
                            dx = zoomed.viewportWidth / 2f - x,
                            dy = zoomed.viewportHeight / 2f - y,
                            anchorX = x,
                            anchorY = y,
                        )
                    } else resized
                }
            }
            .semantics { contentDescription = "Floor plan. Pinch to zoom or use zoom buttons. ${markers.size} markers; use the Markers list for text access." }
            .pointerInput(plan.id, plan.revision, plan.aspectRatio, editing, hitRadius) {
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
            val selectedRoom = roomHighlight
            val selectedRoomColor = highlightColor
            if (selectedRoom != null && selectedRoomColor != null) {
                val (left, top) = t.screen(PlanPoint(selectedRoom.left, selectedRoom.top))
                val (right, bottom) = t.screen(PlanPoint(selectedRoom.right, selectedRoom.bottom))
                val topLeft = Offset(left, top)
                val size = Size(right - left, bottom - top)
                // A dark under-stroke keeps the category-colored outline visible on the
                // similarly orange map fill. Both strokes use the image's PlanTransform.
                drawRect(
                    color = Color(0xFF5A3210),
                    topLeft = topLeft,
                    size = size,
                    style = Stroke(5.dp.toPx())
                )
                drawRect(
                    color = selectedRoomColor,
                    topLeft = topLeft,
                    size = size,
                    style = Stroke(2.5.dp.toPx())
                )
            }
            drawRect(Color.Gray, Offset(t.left, t.top), Size(t.width, t.height), style = Stroke(2.dp.toPx()))
            route?.let { path ->
                path.segments(plan.id).forEach { (from, to) ->
                    val (ax, ay) = t.screen(from)
                    val (bx, by) = t.screen(to)
                    drawLine(Color.White, Offset(ax, ay), Offset(bx, by), 7.dp.toPx(), StrokeCap.Round)
                    drawLine(IndoorRouteColor, Offset(ax, ay), Offset(bx, by), 4.dp.toPx(), StrokeCap.Round)
                }
                val floorNodes = path.nodes.filter { it.floorPlanId == plan.id }
                val endpoints = listOfNotNull(floorNodes.firstOrNull(), floorNodes.lastOrNull()).distinctBy { it.id }
                for (node in endpoints) {
                    val (x, y) = t.screen(node.position)
                    drawCircle(Color.White, 8.dp.toPx(), Offset(x, y))
                    drawCircle(if (node == path.nodes.last()) SunsetOrange else IndoorRouteColor, 5.dp.toPx(), Offset(x, y))
                }
            }
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
        val zoomStyle = ZoomButtonsDefaults.style()
        Surface(
            modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp),
            shape = zoomStyle.shape,
            color = zoomStyle.containerColor,
            contentColor = zoomStyle.contentColor,
        ) {
            Column {
                IconButton(
                    onClick = {
                        transform = transform.gesture(1.35f, 0f, 0f, transform.viewportWidth / 2, transform.viewportHeight / 2)
                    },
                    enabled = transform.zoom < 6f,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(ZoomButtonsDefaults.zoomInPainter(), contentDescription = "Zoom in", modifier = Modifier.size(24.dp))
                }
                HorizontalDivider(modifier = Modifier.size(width = 48.dp, height = zoomStyle.dividerThickness), color = zoomStyle.dividerColor)
                IconButton(
                    onClick = {
                        transform = transform.gesture(1 / 1.35f, 0f, 0f, transform.viewportWidth / 2, transform.viewportHeight / 2)
                    },
                    enabled = transform.zoom > 1f,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(ZoomButtonsDefaults.zoomOutPainter(), contentDescription = "Zoom out", modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
