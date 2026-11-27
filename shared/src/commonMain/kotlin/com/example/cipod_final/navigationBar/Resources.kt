package com.example.cipod_final.navigationBar

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cipod_final.data.CampusResource
import com.example.cipod_final.data.CampusResourceCatalog
import com.example.cipod_final.data.CampusResourceCategory
import com.example.cipod_final.ui.ScreenTitle
import com.example.cipod_final.ui.theme.ChannelBlue
import com.example.cipod_final.ui.theme.Seafoam
import com.example.cipod_final.ui.theme.WarmCream

@Composable
internal fun CampusResourcesScreen(
    hasDirections: (CampusResource) -> Boolean,
    onGetDirections: (CampusResource) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var categoryLabel by rememberSaveable { mutableStateOf(ALL_CATEGORIES) }
    var expandedResourceId by rememberSaveable { mutableStateOf<String?>(null) }
    val category = CampusResourceCategory.entries.firstOrNull { it.label == categoryLabel }
    val resources = CampusResourceCatalog.search(query, category)

    Column(
        Modifier.fillMaxSize().background(WarmCream).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScreenTitle("Campus Resources", Modifier.padding(horizontal = 28.dp, vertical = 12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Search campus resources") },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    TextButton(onClick = { query = "" }) { Text("Clear") }
                }
            },
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = category == null,
                onClick = { categoryLabel = ALL_CATEGORIES },
                label = { Text("All") },
            )
            CampusResourceCategory.entries.forEach { option ->
                FilterChip(
                    selected = category == option,
                    onClick = { categoryLabel = option.label },
                    label = { Text(option.label, maxLines = 1) },
                )
            }
        }
        if (query.isNotEmpty() || category != null) {
            TextButton(onClick = {
                query = ""
                categoryLabel = ALL_CATEGORIES
            }) { Text("Clear search and filters") }
        }

        if (resources.isEmpty()) {
            Text(
                "No campus resources match your search. Try another term or clear the filters.",
                modifier = Modifier.padding(top = 12.dp),
                color = ChannelBlue,
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(start = 2.dp, end = 2.dp, top = 2.dp, bottom = 16.dp),
            ) {
                items(resources, key = { it.id }) { resource ->
                    val expanded = expandedResourceId == resource.id
                    ResourceCard(
                        resource = resource,
                        expanded = expanded,
                        directionsAvailable = hasDirections(resource),
                        onToggle = {
                            expandedResourceId = if (expanded) null else resource.id
                        },
                        onGetDirections = { onGetDirections(resource) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ResourceCard(
    resource: CampusResource,
    expanded: Boolean,
    directionsAvailable: Boolean,
    onToggle: () -> Unit,
    onGetDirections: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Card(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Seafoam, contentColor = ChannelBlue),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(
            Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(resource.name, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(resource.category.label, style = MaterialTheme.typography.labelLarge)
            Text(resource.description, style = MaterialTheme.typography.bodyMedium)
            resource.location?.let { Text("Location: $it", style = MaterialTheme.typography.bodySmall) }
            if (expanded) {
                resource.locationNote?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                resource.contactInfo?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                resource.appointmentInfo?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                TextButton(onClick = { uriHandler.openUri(resource.officialUrl) }) {
                    Text("Official CSUCI website")
                }
                if (directionsAvailable) {
                    Button(onClick = onGetDirections) { Text("Get Directions") }
                } else {
                    val reason = when {
                        resource.location == null -> "A campus location has not been confirmed, so directions are unavailable."
                        else -> "This location is published by CSUCI, but the campus map does not yet have a verified walking destination for this building."
                    }
                    Text(reason, style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Text("Tap for details", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

private const val ALL_CATEGORIES = "All"
