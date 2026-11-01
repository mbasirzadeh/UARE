package com.example.uare.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.uare.snapshot.RuntimeSnapshot
import com.example.uare.ui.components.CodePill
import com.example.uare.ui.viewmodel.ExplorerUiState
import com.example.uare.ui.viewmodel.ExplorerViewModel
import com.example.ui.theme.CyberAmber
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberRed
import com.example.ui.theme.SlateBorder
import com.example.ui.theme.SlateCard
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateTextMuted
import com.example.ui.theme.SlateTextPrimary
import com.example.ui.theme.SlateTextSecondary

@Composable
fun SnapshotScreen(
    state: ExplorerUiState,
    viewModel: ExplorerViewModel
) {
    var snapshotLabel by remember { mutableStateOf("") }
    var selectedBaseSnapshot by remember { mutableStateOf<RuntimeSnapshot?>(null) }
    var selectedTargetSnapshot by remember { mutableStateOf<RuntimeSnapshot?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Capture Snapshot Controls
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateCard),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SlateBorder, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "CREATE BOUNDED RUNTIME SNAPSHOT",
                    color = SlateTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = snapshotLabel,
                        onValueChange = { snapshotLabel = it },
                        placeholder = { Text("Snapshot Label (optional)", color = SlateTextMuted, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f),
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.takeSnapshot(snapshotLabel)
                            snapshotLabel = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Capture", tint = SlateSurface)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Capture", color = SlateSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Compare Button if 2 selected
        if (state.snapshots.size >= 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select 2 snapshots to Diff:",
                    color = SlateTextMuted,
                    fontSize = 11.sp
                )
                Button(
                    onClick = {
                        val base = selectedBaseSnapshot ?: state.snapshots.first()
                        val target = selectedTargetSnapshot ?: state.snapshots.last()
                        viewModel.compareSnapshots(base, target)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberAmber),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CompareArrows, contentDescription = "Diff", tint = SlateSurface)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Diff Snapshots", color = SlateSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Diff Result Card
        val diff = state.selectedDiff
        if (diff != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = SlateSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyberAmber, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "SNAPSHOT DIFF REPORT",
                        color = CyberAmber,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = diff.summary,
                        color = SlateTextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    if (diff.newThreads.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("New Threads: ${diff.newThreads.joinToString()}", color = CyberGreen, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                    if (diff.terminatedThreads.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Terminated Threads: ${diff.terminatedThreads.joinToString()}", color = CyberRed, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Snapshot List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(state.snapshots) { snap ->
                val isBase = selectedBaseSnapshot?.id == snap.id
                val isTarget = selectedTargetSnapshot?.id == snap.id
                Card(
                    colors = CardDefaults.cardColors(containerColor = SlateCard),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (isBase) CyberCyan else if (isTarget) CyberAmber else SlateBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            if (selectedBaseSnapshot == null || (selectedBaseSnapshot != null && selectedTargetSnapshot != null)) {
                                selectedBaseSnapshot = snap
                                selectedTargetSnapshot = null
                            } else {
                                selectedTargetSnapshot = snap
                            }
                        }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${snap.label} (${snap.id})",
                                color = CyberCyan,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            if (isBase) CodePill(text = "BASE", color = CyberCyan)
                            if (isTarget) CodePill(text = "TARGET", color = CyberAmber)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CodePill(text = "${String.format("%.1f", snap.memory.usedMemoryMb)} MB", color = CyberCyan)
                            CodePill(text = "${snap.threads.size} Threads", color = CyberGreen)
                            CodePill(text = "${snap.services.size} Services", color = CyberAmber)
                            CodePill(text = "${snap.classLoaders.size} Loaders", color = SlateTextSecondary)
                        }
                    }
                }
            }
        }
    }
}
