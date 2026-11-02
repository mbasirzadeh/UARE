package com.example.uare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import com.example.uare.api.ExplorationTarget
import com.example.uare.binder.BinderMethodInfo
import com.example.uare.binder.SystemServiceInfo
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

        // Unlimited Nested Exploration Breadcrumb Bar
        if (state.explorationStack.isNotEmpty()) {
            BreadcrumbTrailBar(
                stack = state.explorationStack,
                onBack = { viewModel.popExploration() },
                onJump = { idx -> viewModel.jumpToExplorationIndex(idx) },
                onRoot = { viewModel.resetExplorationToRoot() }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Global Search Bar across all runtime components with Loading Indicator
        OutlinedTextField(
            value = globalSearchQuery,
            onValueChange = {
                globalSearchQuery = it
                viewModel.searchGlobal(it)
            },
            placeholder = { Text("Search classes, objects, services, fields...", color = SlateTextMuted, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyberCyan) },
            trailingIcon = {
                if (state.isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = CyberCyan,
                        strokeWidth = 2.dp
                    )
                } else if (globalSearchQuery.isNotEmpty()) {
                    IconButton(onClick = {
                        globalSearchQuery = ""
                        viewModel.searchGlobal("")
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = SlateTextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            },
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

        // Loading indicator during searches
        if (state.isSearching || state.isLoading) {
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = CyberCyan,
                trackColor = SlateCard
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = if (state.isSearching) "Searching runtime components..." else "Loading components...",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // If user is currently in a nested exploration target, show the deep inspector view
        val currentTarget = state.currentExplorationTarget
        if (currentTarget != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                when (currentTarget) {
                    is ExplorationTarget.ClassTarget -> {
                        val details = state.selectedClassDetails
                        if (details != null && details.className == currentTarget.className) {
                            NestedClassDetailView(
                                details = details,
                                registeredObjects = state.registeredObjects,
                                onUseMethod = { mName ->
                                    viewModel.useMethod(null, details.className, mName)
                                },
                                onUseConstructor = { sig, params ->
                                    viewModel.useConstructor(details.className, sig, params)
                                },
                                onExploreClass = { cName ->
                                    viewModel.exploreClass(cName)
                                },
                                onInspectObject = { handle ->
                                    viewModel.exploreObject(handle)
                                }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = CyberCyan)
                            }
                        }
                    }
                    is ExplorationTarget.ObjectTarget -> {
                        val handle = currentTarget.handle
                        val node = state.selectedObjectNode
                        NestedObjectDetailView(
                            handle = handle,
                            node = node,
                            onUseField = { fName ->
                                viewModel.useField(handle, handle.className, fName)
                            },
                            onDrillDownField = { fName, childNode ->
                                viewModel.drillDownField(handle, fName, childNode)
                            },
                            onExploreType = { typeName ->
                                viewModel.exploreClass(typeName)
                            },
                            onUseMethod = { mName ->
                                viewModel.useMethod(handle, handle.className, mName)
                            }
                        )
                    }
                }
            }
        } else {
            // Unified Category Chips when at root
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
}

@Composable
fun BreadcrumbTrailBar(
    stack: List<ExplorationTarget>,
    onBack: () -> Unit,
    onJump: (Int) -> Unit,
    onRoot: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateCard),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = CyberCyan,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onRoot() }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Root", tint = CyberAmber, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Root", color = CyberAmber, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                stack.forEachIndexed { index, target ->
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = SlateTextMuted,
                        modifier = Modifier.size(14.dp)
                    )

                    val isLast = index == stack.lastIndex
                    Text(
                        text = target.title,
                        color = if (isLast) CyberCyan else SlateTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onJump(index) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            IconButton(
                onClick = onRoot,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Reset to Root",
                    tint = SlateTextMuted,
                    modifier = Modifier.size(16.dp)
                )
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
            text = "LIVE OBJECTS & INSTANCES (${filtered.size})",
            color = SlateTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered) { handle ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SlateBorder, RoundedCornerShape(10.dp))
                        .clickable { viewModel.exploreObject(handle) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    handle.id,
                                    color = CyberCyan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                CodePill(text = "@${Integer.toHexString(handle.identityHash)}", color = CyberAmber)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(handle.className, color = SlateTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            if (handle.description.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(handle.description, color = SlateTextMuted, fontSize = 10.sp)
                            }
                        }

                        if (state.selectionMode.active) {
                            Button(
                                onClick = { viewModel.selectObjectForRunnerParameter(handle) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("SELECT", color = SlateSurface, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        } else {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { viewModel.exploreObject(handle) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("INSPECT", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = { viewModel.setReturnedObjectAsRunnerTarget(handle) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SlateSurface, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("USE", color = SlateSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
fun NestedObjectDetailView(
    handle: ObjectHandle,
    node: ObjectInspectionNode?,
    onUseField: (String) -> Unit,
    onDrillDownField: (String, ObjectInspectionNode) -> Unit,
    onExploreType: (String) -> Unit,
    onUseMethod: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Live Fields & Variables", "Available Methods")

    Column(modifier = Modifier.fillMaxSize()) {
        // Object Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(handle.id, color = CyberCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.width(6.dp))
                        CodePill(text = "@${Integer.toHexString(handle.identityHash)}", color = CyberAmber)
                    }

                    Button(
                        onClick = { onExploreType(handle.className) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberPurple.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("EXPLORE CLASS", color = CyberPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(handle.className, color = SlateTextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                if (handle.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(handle.description, color = SlateTextMuted, fontSize = 11.sp)
                }
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

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            0 -> {
                // Live Fields List
                if (node != null && node.children.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(node.children) { child ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().border(0.5.dp, SlateBorder, RoundedCornerShape(8.dp))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    child.name,
                                                    color = CyberCyan,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = child.type.substringAfterLast('.'),
                                                    color = CyberPurple,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .clickable { onExploreType(child.type) }
                                                        .background(CyberPurple.copy(alpha = 0.15f))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = child.valueString,
                                                color = if (child.isPrimitiveOrWrapper) CyberGreen else CyberAmber,
                                                fontSize = 11.sp,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = 3
                                            )
                                        }

                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            // Contextual USE on Field
                                            Button(
                                                onClick = { onUseField(child.name) },
                                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text("USE", color = SlateSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }

                                            // If field is an object, allow drilling down
                                            if (!child.isPrimitiveOrWrapper) {
                                                Button(
                                                    onClick = { onDrillDownField(child.name, child) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = CyberAmber.copy(alpha = 0.2f)),
                                                    shape = RoundedCornerShape(4.dp)
                                                ) {
                                                    Icon(Icons.Default.ArrowForward, contentDescription = "Drill Down", tint = CyberAmber, modifier = Modifier.size(12.dp))
                                                    Spacer(modifier = Modifier.width(2.dp))
                                                    Text("EXPLORE", color = CyberAmber, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No declared instance fields found on this object.", color = SlateTextMuted, fontSize = 12.sp)
                    }
                }
            }
            1 -> {
                // Available Methods on this Object
                val clazz = try { Class.forName(handle.className) } catch (_: Throwable) { null }
                val methods = clazz?.methods?.filter { it.declaringClass != Any::class.java }?.sortedBy { it.name } ?: emptyList()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(methods) { m ->
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
                                    Text(
                                        text = "${m.name}(${m.parameterTypes.joinToString(", ") { it.simpleName }})",
                                        color = CyberCyan,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = "Return: ${m.returnType.simpleName}",
                                            color = CyberGreen,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable { onExploreType(m.returnType.name) }
                                                .background(CyberGreen.copy(alpha = 0.15f))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onUseMethod(m.name) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SlateSurface, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("USE", color = SlateSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(filtered) { cls ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
                    .clickable { viewModel.exploreClass(cls.name) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cls.simpleName, color = CyberCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text(cls.packageName, color = SlateTextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    }
                    Button(
                        onClick = { viewModel.exploreClass(cls.name) },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text("EXPLORE", color = SlateSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun NestedClassDetailView(
    details: ClassDetails,
    registeredObjects: List<ObjectHandle>,
    onUseMethod: (String) -> Unit,
    onUseConstructor: (String, List<String>) -> Unit,
    onExploreClass: (String) -> Unit,
    onInspectObject: (ObjectHandle) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Methods (${details.methods.size})", "Fields (${details.fields.size})", "Constructors (${details.constructors.size})", "Overview")

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = details.simpleName,
                    color = CyberCyan,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = details.packageName, color = SlateTextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(6.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (details.superclassName != null) {
                        Text(
                            text = "Extends: ${details.superclassName.substringAfterLast('.')}",
                            color = CyberAmber,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onExploreClass(details.superclassName) }
                                .background(CyberAmber.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                    details.interfaces.take(2).forEach { iface ->
                        Text(
                            text = "Impl: ${iface.substringAfterLast('.')}",
                            color = CyberPurple,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onExploreClass(iface) }
                                .background(CyberPurple.copy(alpha = 0.15f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
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

        Spacer(modifier = Modifier.height(8.dp))

        when (selectedTab) {
            0 -> {
                // Methods
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
                                        Text(
                                            text = "Return: ${m.returnType.substringAfterLast('.')}",
                                            color = CyberGreen,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable { onExploreClass(m.returnType) }
                                                .background(CyberGreen.copy(alpha = 0.15f))
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Button(
                                    onClick = { onUseMethod(m.name) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SlateSurface, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("USE", color = SlateSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // Fields
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(details.fields) { f ->
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
                                    Text(text = f.name, color = CyberCyan, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = f.type,
                                        color = CyberPurple,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable { onExploreClass(f.type) }
                                            .background(CyberPurple.copy(alpha = 0.15f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }

                                Button(
                                    onClick = { onExploreClass(f.type) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberPurple.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("EXPLORE TYPE", color = CyberPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // Constructors
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
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
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(text = c.modifiers.joinToString(), color = SlateTextSecondary, fontSize = 10.sp)
                                }
                                Button(
                                    onClick = { onUseConstructor(c.signature, c.parameterTypes) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SlateSurface, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("USE", color = SlateSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                // Overview
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Class: ${details.className}", color = SlateTextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("Modifiers: ${details.modifiers.joinToString()}", color = CyberCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("Superclass: ${details.superclassName ?: "None"}", color = SlateTextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("Interfaces: ${details.interfaces.ifEmpty { listOf("None") }.joinToString()}", color = SlateTextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)

                    val matchingObjects = registeredObjects.filter { it.className == details.className }
                    if (matchingObjects.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Live Instances in Memory (${matchingObjects.size}):", color = CyberGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        matchingObjects.forEach { obj ->
                            Button(
                                onClick = { onInspectObject(obj) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberGreen.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Inspect ${obj.id} (@${Integer.toHexString(obj.identityHash)})", color = CyberGreen, fontSize = 11.sp)
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
            var expanded by remember { mutableStateOf(svc.name == "uare.runtime.service") }

            Card(
                colors = CardDefaults.cardColors(containerColor = SlateCard),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (svc.name == "uare.runtime.service") CyberGreen else SlateBorder,
                        RoundedCornerShape(10.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = svc.name,
                                    color = if (svc.name == "uare.runtime.service") CyberGreen else CyberCyan,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (svc.name == "uare.runtime.service") {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    CodePill(text = "Sample Binder", color = CyberGreen)
                                }
                            }
                            if (svc.interfaceDescriptor != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(text = "AIDL: ${svc.interfaceDescriptor}", color = SlateTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            CodePill(
                                text = if (svc.isPingable) "Ping OK" else "Restricted",
                                color = if (svc.isPingable) CyberGreen else CyberRed
                            )

                            Button(
                                onClick = { expanded = !expanded },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateSurface),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    if (expanded) "Hide Methods" else "Methods (${svc.methods.size})",
                                    color = CyberCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Discovered Binder Methods with contextual [USE] action
                    if (expanded && svc.methods.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "DISCOVERED BINDER METHODS (${svc.methods.size})",
                            color = SlateTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            svc.methods.forEach { method ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateSurface),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.fillMaxWidth().border(0.5.dp, SlateBorder, RoundedCornerShape(6.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = method.signature,
                                                color = CyberCyan,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                CodePill(text = "IPC", color = CyberAmber)
                                                CodePill(text = method.returnType, color = CyberGreen)
                                            }
                                        }

                                        // Contextual USE on Binder Method
                                        Button(
                                            onClick = {
                                                viewModel.useBinderMethod(
                                                    serviceName = svc.name,
                                                    descriptor = svc.interfaceDescriptor,
                                                    methodInfo = method,
                                                    proxyHandleId = svc.proxyObjectHandleId
                                                )
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SlateSurface, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("USE", color = SlateSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
}
