package com.example.uare.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.uare.query.QueryPredicate
import com.example.uare.ui.components.CodePill
import com.example.uare.ui.viewmodel.ExplorerUiState
import com.example.uare.ui.viewmodel.ExplorerViewModel
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary

@Composable
fun QueryConsoleScreen(
    state: ExplorerUiState,
    viewModel: ExplorerViewModel
) {
    val targetTypes = listOf("Classes", "Services", "Threads")
    var selectedTarget by remember { mutableStateOf("Classes") }

    val fieldsForTarget = when (selectedTarget) {
        "Classes" -> listOf("name", "package", "superclass", "loader")
        "Services" -> listOf("name", "descriptor", "class")
        else -> listOf("name", "state", "group")
    }

    var selectedField by remember { mutableStateOf(fieldsForTarget.first()) }
    var selectedOperator by remember { mutableStateOf(QueryPredicate.Operator.CONTAINS) }
    var queryValue by remember { mutableStateOf("Activity") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "RUNTIME QUERY CONSOLE",
            color = SlateTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Structured predicate evaluation across loaded classes, binder services, and threads.",
            color = SlateTextMuted,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Target Selector
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "TARGET ENTITY",
                    color = SlateTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    targetTypes.forEach { target ->
                        val isSel = selectedTarget == target
                        FilterChip(
                            selected = isSel,
                            onClick = {
                                selectedTarget = target
                                selectedField = when (target) {
                                    "Classes" -> "name"
                                    "Services" -> "name"
                                    else -> "name"
                                }
                            },
                            label = { Text(target, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                                selectedLabelColor = CyberCyan,
                                containerColor = SlateSurface,
                                labelColor = SlateTextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Field Selector
                Text(
                    text = "FIELD: $selectedField | OP: ${selectedOperator.name}",
                    color = SlateTextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    fieldsForTarget.forEach { fld ->
                        val isSel = selectedField == fld
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedField = fld },
                            label = { Text(fld, fontSize = 11.sp, fontFamily = FontFamily.Monospace) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CyberAmber.copy(alpha = 0.2f),
                                selectedLabelColor = CyberAmber,
                                containerColor = SlateSurface,
                                labelColor = SlateTextSecondary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = queryValue,
                    onValueChange = { queryValue = it },
                    label = { Text("Search Pattern / Value", fontSize = 11.sp) },
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

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        viewModel.executeQuery(selectedField, selectedOperator, queryValue.trim(), selectedTarget)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run Query", tint = SlateSurface)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Execute Structured Query", color = SlateSurface, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Query Results
        val qr = state.lastQueryResult
        if (qr != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RESULTS: ${qr.totalFound} MATCHES",
                            color = CyberGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        CodePill(text = "${qr.executionTimeMs} ms", color = CyberCyan)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val resultsList = when {
                        qr.matchedClassNames.isNotEmpty() -> qr.matchedClassNames
                        qr.matchedServiceNames.isNotEmpty() -> qr.matchedServiceNames
                        else -> qr.matchedThreadNames
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(resultsList) { item ->
                            Text(
                                text = "• $item",
                                color = SlateTextPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
