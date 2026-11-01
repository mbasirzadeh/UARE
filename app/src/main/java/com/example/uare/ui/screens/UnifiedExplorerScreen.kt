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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.example.uare.discovery.DiscoveredClass
import com.example.uare.inspection.ClassDetails
import com.example.uare.inspection.ObjectHandle
import com.example.uare.inspection.ObjectInspectionNode
import com.example.uare.ui.components.CodePill
import com.example.uare.ui.viewmodel.ActiveScreen
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

enum class UnifiedCategory(val label: String) {
    OBJECTS("Objects & Instances"),
    CLASSES("Classes & DEX"),
    SERVICES("Binder & Services"),
    LOADERS("ClassLoaders")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun UnifiedExplorerScreen(
    state: ExplorerUiState,
    viewModel: ExplorerViewModel
) {
    var selectedCategory by remember { mutableStateOf(UnifiedCategory.OBJECTS) }
    var globalSearchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        // Selection Mode Banner if user clicked [BROWSE] in Runner
        if (state.selectionMode.active) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CyberAmber.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberAmber, RoundedCornerShape(10.dp))
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SELECTION MODE: BROWSE FOR RUNNER",
                            color = CyberAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = state.selectionMode.prompt,
                            color = SlateTextPrimary,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = { viewModel.cancelSelectionMode() },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateCard)
                    ) {
                        Text("Cancel", color = SlateTextSecondary, fontSize = 11.sp)
                    }
                }
            }
        }

        // Global Search Bar across all runtime components
        OutlinedTextField(
            value = globalSearchQuery,
            onValueChange = {
                globalSearchQuery = it
                if (selectedCategory == UnifiedCategory.CLASSES) {
                    viewModel.searchClasses(it)
                }
            },
            placeholder = { Text("Search classes, objects, services, fields...", color = SlateTextMuted, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan) },
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

        // Unified Category Chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            UnifiedCategory.values().forEach { cat ->
                val isSelected = selectedCategory == cat
                val count = when (cat) {
                    UnifiedCategory.OBJECTS -> state.registeredObjects.size
                    UnifiedCategory.CLASSES -> state.discoveredClasses.size
                    UnifiedCategory.SERVICES -> state.services.size
                    UnifiedCategory.LOADERS -> state.classLoaders.size
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = cat },
                    label = { Text("${cat.label} ($count)", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
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

        // Dynamic Main Section
        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedCategory) {
                UnifiedCategory.OBJECTS -> UnifiedObjectsView(
                    state = state,
                    viewModel = viewModel,
                    filter = globalSearchQuery
                )
                UnifiedCategory.CLASSES -> UnifiedClassesView(
                    state = state,
                    viewModel = viewModel,
                    filter = globalSearchQuery
                )
                UnifiedCategory.SERVICES -> UnifiedServicesView(
                    state = state,
                    viewModel = viewModel,
                    filter = globalSearchQuery
                )
                UnifiedCategory.LOADERS -> ClassLoaderScreen(state = state)
            }
        }
    }
}

@Composable
fun UnifiedObjectsView(
    state: ExplorerUiState,
    viewModel: ExplorerViewModel,
    filter: String
) {
    val filtered = state.registeredObjects.filter {
        filter.isEmpty() || it.id.contains(filter, ignoreCase = true) || it.className.contains(filter, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "REGISTERED INSTANCES & INTERACTIVE OBJECT GRAPH",
            color = SlateTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))

        // Split view: objects on top, selected object inspector below
        LazyColumn(
            modifier = Modifier.weight(0.42f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(filtered) { handle ->
                val isSelected = state.selectedObjectHandle?.id == handle.id
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) CyberCyan.copy(alpha = 0.15f) else SlateCard
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (isSelected) CyberCyan else SlateBorder, RoundedCornerShape(8.dp))
                        .clickable { viewModel.inspectObject(handle) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(handle.id, color = CyberCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(handle.className, color = SlateTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            if (handle.description.isNotEmpty()) {
                                Text(handle.description, color = SlateTextMuted, fontSize = 10.sp)
                            }
                        }

                        // If in Selection Mode: Show [SELECT FOR PARAMETER] button
                        if (state.selectionMode.active) {
                            Button(
                                onClick = { viewModel.selectObjectForRunnerParameter(handle) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("SELECT", color = SlateSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        } else {
                            CodePill(text = "@${Integer.toHexString(handle.identityHash)}", color = CyberAmber)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Selected Object Detail & Operations
        val selHandle = state.selectedObjectHandle
        val selNode = state.selectedObjectNode

        Card(
            colors = CardDefaults.cardColors(containerColor = SlateSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.58f)
                .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
        ) {
            if (selHandle != null && selNode != null) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "FIELDS ON ${selHandle.id}",
                            color = SlateTextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        // Contextual USE on this object's methods
                        Button(
                            onClick = {
                                viewModel.useMethod(
                                    targetHandle = selHandle,
                                    targetClassName = selHandle.className,
                                    methodName = "toString"
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SlateSurface, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("USE IN RUNNER", color = SlateSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(selNode.children) { child ->
                            InteractiveFieldRow(
                                handle = selHandle,
                                node = child,
                                onUseField = { fieldName ->
                                    viewModel.useField(selHandle, selHandle.className, fieldName)
                                },
                                onDrillDown = { objId ->
                                    val targetH = state.registeredObjects.find { it.id == objId }
                                    if (targetH != null) viewModel.inspectObject(targetH)
                                }
                            )
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Select an object to inspect interactive fields and methods", color = SlateTextMuted, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun InteractiveFieldRow(
    handle: ObjectHandle,
    node: ObjectInspectionNode,
    onUseField: (String) -> Unit,
    onDrillDown: (String) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth().border(0.5.dp, SlateBorder, RoundedCornerShape(6.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = node.name,
                        color = CyberCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    CodePill(text = node.type, color = SlateTextMuted)
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = node.valueString,
                    color = if (node.isCircularReference) CyberRed else if (node.isPrimitiveOrWrapper) CyberGreen else CyberAmber,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2
                )
            }

            // Contextual [USE] Action on field
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { onUseField(node.name) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text("USE", color = SlateSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                if (!node.isPrimitiveOrWrapper && node.children.isNotEmpty()) {
                    IconButton(
                        onClick = { onUseField(node.name) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription = "Drill Down", tint = CyberAmber, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun UnifiedClassesView(
    state: ExplorerUiState,
    viewModel: ExplorerViewModel,
    filter: String
) {
    val filtered = state.discoveredClasses.filter {
        filter.isEmpty() || it.name.contains(filter, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filtered) { clazz ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
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
                        CodePill(text = clazz.source.name, color = CyberGreen)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = clazz.name, color = SlateTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.inspectClass(clazz.name) },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Inspect Members", fontSize = 11.sp, color = SlateTextPrimary)
                        }
                        Spacer(modifier = Modifier.width(6.dp))

                        // Contextual [USE] Action on Class
                        Button(
                            onClick = {
                                viewModel.useMethod(
                                    targetHandle = null,
                                    targetClassName = clazz.name,
                                    methodName = ""
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("USE", color = SlateSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal Member Inspector with contextual USE beside every method and constructor
    if (state.selectedClassDetails != null) {
        ClassInspectorWithUseDialog(
            details = state.selectedClassDetails,
            onDismiss = { viewModel.clearSelectedClass() },
            onUseMethod = { methodName ->
                viewModel.clearSelectedClass()
                viewModel.useMethod(null, state.selectedClassDetails.fullyQualifiedName, methodName)
            },
            onUseConstructor = { sig, params ->
                viewModel.clearSelectedClass()
                viewModel.useConstructor(state.selectedClassDetails.fullyQualifiedName, sig, params)
            }
        )
    }
}

@Composable
fun ClassInspectorWithUseDialog(
    details: ClassDetails,
    onDismiss: () -> Unit,
    onUseMethod: (String) -> Unit,
    onUseConstructor: (String, List<String>) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(1) } // Default to Methods
    val tabs = listOf("Overview", "Methods (${details.methods.size})", "Constructors (${details.constructors.size})", "Fields (${details.fields.size})")

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = details.simpleName,
                            color = SlateTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(text = details.packageName, color = SlateTextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
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
                        TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = CyberCyan)
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
                            }
                        }
                        1 -> { // Methods with contextual USE
                            items(details.methods) { m ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().border(0.5.dp, SlateBorder, RoundedCornerShape(8.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = m.signature, color = CyberCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                if (m.isStatic) CodePill(text = "static", color = CyberAmber)
                                                CodePill(text = m.returnType.substringAfterLast('.'), color = CyberGreen)
                                            }
                                        }
                                        Button(
                                            onClick = { onUseMethod(m.name) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("USE", color = SlateSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        2 -> { // Constructors with contextual USE
                            items(details.constructors) { c ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().border(0.5.dp, SlateBorder, RoundedCornerShape(8.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = c.signature, color = CyberAmber, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = c.modifiers.joinToString(), color = SlateTextSecondary, fontSize = 10.sp)
                                        }
                                        Button(
                                            onClick = { onUseConstructor(c.signature, c.parameterTypes) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text("USE", color = SlateSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        3 -> { // Fields
                            items(details.fields) { f ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().border(0.5.dp, SlateBorder, RoundedCornerShape(8.dp))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(text = "${f.type} ${f.name}", color = CyberGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
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

@Composable
fun UnifiedServicesView(
    state: ExplorerUiState,
    viewModel: ExplorerViewModel,
    filter: String
) {
    val filtered = state.services.filter {
        filter.isEmpty() || it.name.contains(filter, ignoreCase = true) || (it.interfaceDescriptor?.contains(filter, ignoreCase = true) == true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(filtered) { svc ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = svc.name,
                            color = CyberCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CodePill(
                                text = if (svc.isPingable) "Ping OK" else "Unresponsive",
                                color = if (svc.isPingable) CyberGreen else CyberRed
                            )

                            // Contextual [USE] Action on Binder Service
                            Button(
                                onClick = {
                                    viewModel.useBinderOperation(
                                        serviceName = svc.name,
                                        descriptor = svc.interfaceDescriptor,
                                        operationName = "pingBinder"
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text("USE", color = SlateSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (svc.interfaceDescriptor != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "AIDL: ${svc.interfaceDescriptor}", color = SlateTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Class: ${svc.binderClassName}", color = SlateTextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}
