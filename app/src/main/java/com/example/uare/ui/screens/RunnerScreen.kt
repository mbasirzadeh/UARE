package com.example.uare.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.getValue
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
import com.example.uare.api.RuntimeOperation
import com.example.uare.runner.ParameterSpec
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RunnerScreen(
    state: ExplorerUiState,
    viewModel: ExplorerViewModel
) {
    val runner = state.activeRunner

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
                                text = "In Explorer, tap [USE] on any method, constructor, field, or Binder method to configure it here.",
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
                                    is RuntimeOperation.InvokeBinderOperation -> op.proxyHandleId
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
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )

                            // Method Switcher within Target
                            if (runner.availableMethods.size > 1) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "SELECT METHOD ON TARGET:",
                                    color = SlateTextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val currentMethodName = when (val op = runner.operation) {
                                        is RuntimeOperation.InvokeMethod -> op.methodName
                                        is RuntimeOperation.InvokeBinderOperation -> op.operationName
                                        else -> ""
                                    }

                                    runner.availableMethods.take(8).forEach { mName ->
                                        val isSelected = mName == currentMethodName
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { viewModel.switchRunnerMethod(mName) },
                                            label = { Text(mName, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                                selectedLabelColor = CyberCyan,
                                                containerColor = SlateSurface,
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
                            }
                        }
                    }
                }

                // Dynamic Body based on Operation Type
                when (val op = runner.operation) {
                    is RuntimeOperation.InvokeMethod, is RuntimeOperation.CreateInstance, is RuntimeOperation.InvokeBinderOperation -> {
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
                                        Text("No arguments required for this operation.", color = CyberGreen, fontSize = 12.sp)
                                    } else {
                                        runner.parameterSpecs.forEach { spec ->
                                            ParameterInputCard(
                                                spec = spec,
                                                currentInput = runner.arguments[spec.index],
                                                registeredObjects = state.registeredObjects,
                                                onValueChange = { valStr -> viewModel.setRunnerArgumentValue(spec.index, valStr) },
                                                onSetNull = { isNull -> viewModel.setRunnerArgumentNull(spec.index, isNull) },
                                                onSelectObject = { handle ->
                                                    viewModel.setRunnerArgumentValue(spec.index, "@${handle.id}")
                                                    val arg = runner.arguments[spec.index]
                                                    if (arg != null) {
                                                        runner.arguments[spec.index] = arg.copy(selectedObjectHandleId = handle.id, rawValue = "@${handle.id}", isNull = false)
                                                    }
                                                },
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

                                    // Safety confirmation checkbox
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
                                            Text("EXECUTE OPERATION", color = SlateSurface, fontWeight = FontWeight.Bold)
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

                                // Exception Viewer with Stack Trace
                                if (record.exceptionMessage != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = CyberRed.copy(alpha = 0.1f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().border(1.dp, CyberRed.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp)) {
                                            Text(
                                                text = "Exception: ${record.exceptionMessage}",
                                                color = CyberRed,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            if (record.stackTrace.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text("Stack Trace:", color = SlateTextMuted, fontSize = 10.sp)
                                                record.stackTrace.take(6).forEach { frame ->
                                                    Text(text = "  at $frame", color = SlateTextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                                }
                                            }
                                        }
                                    }
                                }

                                // Interactive Actions on Returned Object
                                if (record.returnObjectHandle != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = { viewModel.openObjectInExplorer(record.returnObjectHandle) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = SlateSurface, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Open in Explorer (${record.returnObjectHandle.id})",
                                                color = SlateSurface,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }

                                        Button(
                                            onClick = { viewModel.setReturnedObjectAsRunnerTarget(record.returnObjectHandle) },
                                            colors = ButtonDefaults.buttonColors(containerColor = CyberPurple),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = SlateSurface, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Set as Runner Target",
                                                color = SlateSurface,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = hist.methodName,
                                    color = CyberCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = hist.targetClassName.substringAfterLast('.'),
                                    color = SlateTextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                CodePill(text = "${hist.durationMs}ms", color = if (hist.isSuccess) CyberGreen else CyberRed)
                                Button(
                                    onClick = { viewModel.reloadArgumentsFromHistory(hist) },
                                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text("RELOAD", color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
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
    onSetNull: (Boolean) -> Unit,
    onSelectObject: (com.example.uare.inspection.ObjectHandle) -> Unit,
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

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!spec.isPrimitive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = currentInput?.isNull == true,
                            onCheckedChange = onSetNull,
                            colors = CheckboxDefaults.colors(checkedColor = CyberAmber, checkmarkColor = SlateSurface)
                        )
                        Text("null", color = SlateTextMuted, fontSize = 10.sp)
                    }
                }

                if (spec.isObjectReference || !spec.isPrimitive) {
                    Spacer(modifier = Modifier.width(6.dp))
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
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (currentInput?.isNull == true) {
            Text("Argument set to null", color = SlateTextMuted, fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
        } else {
            when {
                spec.isEnum -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        spec.enumConstants.forEach { enumVal ->
                            val isSel = currentInput?.rawValue == enumVal
                            FilterChip(
                                selected = isSel,
                                onClick = { onValueChange(enumVal) },
                                label = { Text(enumVal, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = CyberCyan,
                                    containerColor = SlateCard,
                                    labelColor = SlateTextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected(isSel)) CyberCyan else SlateBorder,
                                    enabled = true,
                                    selected = isSel
                                )
                            )
                        }
                    }
                }

                spec.isPrimitive && spec.simpleTypeName.contains("bool", ignoreCase = true) -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val isTrue = currentInput?.rawValue.equals("true", ignoreCase = true)
                        FilterChip(
                            selected = isTrue,
                            onClick = { onValueChange("true") },
                            label = { Text("true", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberGreen.copy(alpha = 0.2f),
                                selectedLabelColor = CyberGreen,
                                containerColor = SlateCard,
                                labelColor = SlateTextSecondary
                            )
                        )
                        FilterChip(
                            selected = !isTrue,
                            onClick = { onValueChange("false") },
                            label = { Text("false", fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberRed.copy(alpha = 0.2f),
                                selectedLabelColor = CyberRed,
                                containerColor = SlateCard,
                                labelColor = SlateTextSecondary
                            )
                        )
                    }
                }

                spec.isObjectReference -> {
                    val selectedObj = currentInput?.selectedObjectHandleId
                    Column {
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
                                    Text("Bound Object: $selectedObj", color = CyberGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                    Text("Tap [BROWSE] to re-select", color = SlateTextMuted, fontSize = 10.sp)
                                }
                            }
                        } else {
                            Text("No object selected. Tap [BROWSE] to pick from Explorer, or choose below:", color = SlateTextMuted, fontSize = 11.sp)
                        }

                        // Quick selector from registered objects
                        if (registeredObjects.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                registeredObjects.forEach { obj ->
                                    val isMatch = obj.id == selectedObj
                                    FilterChip(
                                        selected = isMatch,
                                        onClick = { onSelectObject(obj) },
                                        label = { Text(obj.id, fontSize = 10.sp, fontFamily = FontFamily.Monospace) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = CyberGreen.copy(alpha = 0.2f),
                                            selectedLabelColor = CyberGreen,
                                            containerColor = SlateCard,
                                            labelColor = SlateTextSecondary
                                        )
                                    )
                                }
                            }
                        }
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
}

private fun isSelected(value: Boolean): Boolean = value
