package com.example.uare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.uare.discovery.ClassDiscoverySource
import com.example.uare.discovery.DiscoveredClass
import com.example.uare.inspection.ClassDetails
import com.example.uare.ui.components.CodePill
import com.example.uare.ui.viewmodel.ExplorerUiState
import com.example.uare.ui.viewmodel.ExplorerViewModel
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.CyberRed
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ClassExplorerScreen(
    state: ExplorerUiState,
    viewModel: ExplorerViewModel
) {
    var searchQuery by remember { mutableStateOf(state.classSearchQuery) }
    var selectedPackageFilter by remember { mutableStateOf(state.packageFilter) }

    val packageShortcuts = listOf(
        "All" to "",
        "android.app" to "android.app",
        "android.os" to "android.os",
        "java.lang" to "java.lang",
        "com.android.server" to "com.android.server",
        "com.example" to "com.example"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Input
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                viewModel.searchClasses(it, selectedPackageFilter)
            },
            placeholder = { Text("Search class or package...", color = SlateTextMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = CyberCyan) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SlateCard,
                unfocusedContainerColor = SlateCard,
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = SlateBorder,
                focusedTextColor = SlateTextPrimary,
                unfocusedTextColor = SlateTextPrimary
            ),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Package shortcut chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            packageShortcuts.forEach { (label, pkg) ->
                val isSelected = selectedPackageFilter == pkg
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedPackageFilter = pkg
                        viewModel.searchClasses(searchQuery, pkg)
                    },
                    label = { Text(label, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                        selectedLabelColor = CyberCyan,
                        containerColor = SlateCard,
                        labelColor = SlateTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = if (isSelected) CyberCyan else SlateBorder,
                        enabled = true,
                        selected = isSelected
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DISCOVERED CLASSES (${state.discoveredClasses.size})",
                color = SlateTextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CyberCyan, strokeWidth = 2.dp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(state.discoveredClasses) { clazz ->
                ClassItemCard(
                    clazz = clazz,
                    onClick = { viewModel.inspectClass(clazz.name) }
                )
            }
        }
    }

    // Modal Class Inspector Dialog
    if (state.selectedClassDetails != null) {
        ClassInspectorDialog(
            details = state.selectedClassDetails,
            onDismiss = { viewModel.clearSelectedClass() }
        )
    }
}

@Composable
fun ClassItemCard(
    clazz: DiscoveredClass,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = clazz.simpleName,
                    color = CyberCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                val sourceColor = when (clazz.source) {
                    ClassDiscoverySource.LOADED_CLASS -> CyberGreen
                    ClassDiscoverySource.DEX_ENTRY -> CyberAmber
                    ClassDiscoverySource.PLATFORM_INDEX -> CyberPurple
                    ClassDiscoverySource.REFLECTION -> CyberCyan
                }
                CodePill(text = clazz.source.name, color = sourceColor)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = clazz.name,
                color = SlateTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (clazz.isInterface) CodePill(text = "interface", color = CyberPurple)
                if (clazz.isAbstract && !clazz.isInterface) CodePill(text = "abstract", color = CyberAmber)
                if (clazz.isEnum) CodePill(text = "enum", color = CyberGreen)
                if (clazz.isFinal) CodePill(text = "final", color = SlateTextMuted)
                if (clazz.classLoaderName != null) {
                    CodePill(text = clazz.classLoaderName, color = SlateTextSecondary)
                }
            }
        }
    }
}

@Composable
fun ClassInspectorDialog(
    details: ClassDetails,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Methods (${details.methods.size})", "Fields (${details.fields.size})", "Constructors (${details.constructors.size})")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(600.dp)
                .border(1.dp, SlateBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = details.simpleName,
                            color = SlateTextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = details.packageName,
                            color = SlateTextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = SlateTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = SlateCard,
                    contentColor = CyberCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = CyberCyan
                        )
                    }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(title, fontSize = 11.sp, maxLines = 1) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (selectedTab) {
                        0 -> { // Overview
                            item {
                                Text("Modifiers: ${details.modifiers.joinToString()}", color = CyberCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Superclass: ${details.superclassName ?: "None"}", color = SlateTextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Interfaces: ${details.interfaces.ifEmpty { listOf("None") }.joinToString()}", color = SlateTextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("ClassLoader: ${details.classLoaderName ?: "Unknown"}", color = CyberAmber, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                if (details.annotations.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Annotations: ${details.annotations.joinToString()}", color = CyberPurple, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                        1 -> { // Methods
                            items(details.methods) { m ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().border(0.5.dp, SlateBorder, RoundedCornerShape(8.dp))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(text = m.signature, color = CyberCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (m.isStatic) CodePill(text = "static", color = CyberAmber)
                                            if (m.isSynthetic) CodePill(text = "synthetic", color = SlateTextMuted)
                                            CodePill(text = m.modifiers.joinToString(), color = SlateTextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                        2 -> { // Fields
                            items(details.fields) { f ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().border(0.5.dp, SlateBorder, RoundedCornerShape(8.dp))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(text = "${f.type} ${f.name}", color = CyberGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (f.isStatic) CodePill(text = "static", color = CyberAmber)
                                            if (f.isFinal) CodePill(text = "final", color = SlateTextMuted)
                                            CodePill(text = f.modifiers.joinToString(), color = SlateTextSecondary)
                                        }
                                    }
                                }
                            }
                        }
                        3 -> { // Constructors
                            items(details.constructors) { c ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().border(0.5.dp, SlateBorder, RoundedCornerShape(8.dp))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(text = c.signature, color = CyberAmber, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = c.modifiers.joinToString(), color = SlateTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
