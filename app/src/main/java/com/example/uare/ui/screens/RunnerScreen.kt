package com.example.uare.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uare.api.RuntimeOperation
import com.example.uare.runner.ParameterSpec
import com.example.uare.ui.components.CodePill
import com.example.uare.ui.viewmodel.ActiveRunnerState
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunnerScreen(
    state: ExplorerUiState,
    viewModel: ExplorerViewModel
) {
    val runner = state.activeRunner

    // Android hardware/gesture back returns to Explorer
    BackHandler {
        viewModel.navigateTo(ActiveScreen.EXPLORER)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "RUNTIME RUNNER",
                            color = SlateTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = runner?.operation?.operationType ?: "Execution Console",
                            color = CyberAmber,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(ActiveScreen.EXPLORER) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Return to Explorer",
                            tint = CyberCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SlateSurface)
            )
        },
        containerColor = SlateSurface
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (runner == null) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No Operation Configured",
                                color = SlateTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "In Explorer, tap [USE] on any method, constructor, or field to configure it here.",
                                color = SlateTextSecondary,
                                fontSize = 12.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = { viewModel.navigateTo(ActiveScreen.EXPLORER) },
                                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
                            ) {
                                Text("Go to Explorer", color = SlateSurface, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                // Operation Header Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CodePill(text = runner.operation.operationType, color = CyberAmber)
                                val targetHandleId = when (val op = runner.operation) {
                                    is RuntimeOperation.InvokeMethod -> op.target?.id
                                    is RuntimeOperation.WriteField -> op.target.id
                                    else -> null
                                }
                                if (targetHandleId != null) {
                                    CodePill(text = targetHandleId, color = CyberCyan)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = runner.operation.targetClassName,
                                color = SlateTextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = runner.operation.title,
                                color = CyberCyan,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Dynamic Body based on Operation Type
                when (val op = runner.operation) {
                    is RuntimeOperation.InvokeMethod, is RuntimeOperation.CreateInstance -> {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "PARAMETER CONFIGURATION (${runner.parameterSpecs.size} PARAMETERS)",
                                        color = SlateTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (runner.parameterSpecs.isEmpty()) {
                                        Text("No arguments required.", color = CyberGreen, fontSize = 12.sp)
                                    } else {
                                        runner.parameterSpecs.forEach { spec ->
                                            ParameterInputCard(
                                                spec = spec,
                                                currentInput = runner.arguments[spec.index],
                                                registeredObjects = state.registeredObjects,
                                                onValueChange = { valStr -> viewModel.setRunnerArgumentValue(spec.index, valStr) },
                                                onBrowse = {
                                                    viewModel.startObjectBrowseForParameter(
                                                        paramIndex = spec.index,
                                                        expectedType = spec.simpleTypeName,
                                                        paramName = spec.name
                                                    )
                                                }
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Safety confirmation
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Checkbox(
                                            checked = runner.isConfirmedSafe,
                                            onCheckedChange = { viewModel.setRunnerSafetyConfirmed(it) },
                                            colors = CheckboxDefaults.colors(checkedColor = CyberAmber, checkmarkColor = SlateSurface)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "I authorize runtime invocation of this operation",
                                            color = if (runner.isConfirmedSafe) CyberAmber else SlateTextMuted,
                                            fontSize = 11.sp
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Button(
                                        onClick = { viewModel.executeRunner() },
                                        enabled = runner.isConfirmedSafe && !runner.isExecuting,
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        if (runner.isExecuting) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SlateSurface, strokeWidth = 2.dp)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Executing...", color = SlateSurface, fontWeight = FontWeight.Bold)
                                        } else {
                                            Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = SlateSurface)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("EXECUTE", color = SlateSurface, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    is RuntimeOperation.WriteField -> {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "FIELD MUTATION CONTROLS",
                                        color = SlateTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Text(
                                        text = "Current Value: ${runner.fieldCurrentValue}",
                                        color = CyberGreen,
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = runner.fieldEditValue,
                                        onValueChange = { viewModel.setRunnerFieldEditValue(it) },
                                        label = { Text("New Field Value (${op.fieldType})", fontSize = 11.sp) },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = SlateSurface,
                                            unfocusedContainerColor = SlateSurface,
                                            focusedBorderColor = CyberCyan,
                                            unfocusedBorderColor = SlateBorder,
                                            focusedTextColor = SlateTextPrimary,
                                            unfocusedTextColor = SlateTextPrimary
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = { viewModel.executeRunner() },
                                        enabled = !runner.isExecuting,
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("APPLY FIELD MODIFICATION", color = SlateSurface, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    is RuntimeOperation.InvokeBinderOperation -> {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SlateCard),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = "BINDER ENDPOINT CALL",
                                        color = SlateTextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Service: ${op.serviceName}", color = CyberCyan, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                    Text("Descriptor: ${op.descriptor ?: "None"}", color = SlateTextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.executeRunner() },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("PING & QUERY INTERFACE", color = SlateSurface, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                // Execution Result Viewer
                val record = runner.lastExecutionRecord
                if (record != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateSurface),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, if (record.isSuccess) CyberGreen else CyberRed, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (record.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                            contentDescription = null,
                                            tint = if (record.isSuccess) CyberGreen else CyberRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (record.isSuccess) "EXECUTION SUCCESS" else "EXECUTION FAILED",
                                            color = if (record.isSuccess) CyberGreen else CyberRed,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    CodePill(text = "${record.durationMs} ms", color = CyberCyan)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = record.resultString,
                                        color = if (record.isSuccess) CyberGreen else CyberRed,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }

                                if (record.exceptionMessage != null) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("Exception: ${record.exceptionMessage}", color = CyberRed, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                }

                                // Open in Explorer action for returned objects
                                if (record.returnObjectHandle != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = { viewModel.openObjectInExplorer(record.returnObjectHandle) },
                                        colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = SlateSurface)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Open Returned Object in Explorer (${record.returnObjectHandle.id})",
                                            color = SlateSurface,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Execution History Section
            item {
                Text(
                    text = "EXECUTION HISTORY (${state.executionHistory.size})",
                    color = SlateTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            items(state.executionHistory) { hist ->
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
                                text = "${hist.methodName}(${hist.argumentsSummary})",
                                color = CyberCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            CodePill(text = "${hist.durationMs}ms", color = if (hist.isSuccess) CyberGreen else CyberRed)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Result: ${hist.resultString.take(120)}",
                            color = if (hist.isSuccess) CyberGreen else CyberRed,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ParameterInputCard(
    spec: ParameterSpec,
    currentInput: com.example.uare.runner.ArgumentInput?,
    registeredObjects: List<com.example.uare.inspection.ObjectHandle>,
    onValueChange: (String) -> Unit,
    onBrowse: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SlateSurface, RoundedCornerShape(8.dp))
            .border(1.dp, SlateBorder, RoundedCornerShape(8.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${spec.name}:",
                    color = SlateTextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(6.dp))
                CodePill(text = spec.simpleTypeName, color = CyberAmber)
            }

            // Explicit BROWSE Button beside parameter
            if (spec.isObjectReference || !spec.isPrimitive) {
                OutlinedButton(
                    onClick = onBrowse,
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Browse", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("BROWSE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        when {
            spec.isEnum -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    spec.enumConstants.take(5).forEach { enumVal ->
                        val isSel = currentInput?.rawValue == enumVal
                        FilterChip(
                            selected = isSel,
                            onClick = { onValueChange(enumVal) },
                            label = { Text(enumVal, fontSize = 10.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                selectedLabelColor = CyberCyan,
                                containerColor = SlateCard,
                                labelColor = SlateTextSecondary
                            )
                        )
                    }
                }
            }

            spec.isObjectReference -> {
                val selectedObj = currentInput?.selectedObjectHandleId
                if (selectedObj != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateCard),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Bound: $selectedObj", color = CyberGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            Text("Tap BROWSE to change", color = SlateTextMuted, fontSize = 10.sp)
                        }
                    }
                } else {
                    Text("No object selected. Tap [BROWSE] to select an object from Explorer.", color = SlateTextMuted, fontSize = 11.sp)
                }
            }

            else -> {
                OutlinedTextField(
                    value = currentInput?.rawValue ?: "",
                    onValueChange = onValueChange,
                    placeholder = { Text("Enter ${spec.simpleTypeName}", color = SlateTextMuted, fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SlateCard,
                        unfocusedContainerColor = SlateCard,
                        focusedBorderColor = CyberCyan,
                        unfocusedBorderColor = SlateBorder,
                        focusedTextColor = SlateTextPrimary,
                        unfocusedTextColor = SlateTextPrimary
                    )
                )
            }
        }
    }
}
