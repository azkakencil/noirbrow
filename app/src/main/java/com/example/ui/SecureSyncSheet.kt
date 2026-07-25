package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecureSyncSheet(
    onExportPayload: suspend (String) -> String,
    onImportPayload: suspend (String, String) -> Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var passphrase by remember { mutableStateOf("NOIR-PASS-2026") }
    var generatedPayload by remember { mutableStateOf("") }
    var importPayloadText by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SINKRONISASI AMAN (E2EE)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Sinkronkan riwayat & bookmark antar perangkat menggunakan enkripsi AES-256 tersetempel sandi rahasia.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Passphrase Field
            Text(
                text = "KATA SANDI ENKRIPSI BERSAMA:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = passphrase,
                onValueChange = { passphrase = it },
                placeholder = { Text("Ketik kata sandi sync...", fontSize = 12.sp) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sync_passphrase_input"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))
            Divider(color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(20.dp))

            // SECTION 1: EXPORT / GENERATE PAYLOAD
            Text(
                text = "1. EKSPOR KODE SYNC PERANGKAT INI",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (passphrase.isBlank()) {
                        Toast.makeText(context, "Kata sandi enkripsi tidak boleh kosong", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isGenerating = true
                    coroutineScope.launch {
                        generatedPayload = onExportPayload(passphrase)
                        isGenerating = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("generate_sync_code_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Buat Kode Enkripsi Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (generatedPayload.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "KODE SYNC TERENKRIPSI:",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = generatedPayload,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 4,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Noir Sync Code", generatedPayload)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Kode Sync Berhasil Disalin!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .align(Alignment.End)
                                .testTag("copy_sync_code_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Salin Kode", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECTION 2: IMPORT PAYLOAD
            Text(
                text = "2. IMPOR KODE DARI PERANGKAT LAIN",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = importPayloadText,
                onValueChange = { importPayloadText = it },
                placeholder = { Text("Tempel kode NOIR_SYNC di sini...", fontSize = 11.sp) },
                maxLines = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("import_sync_payload_input"),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (importPayloadText.isBlank() || passphrase.isBlank()) {
                        Toast.makeText(context, "Tempel kode sync dan isi kata sandi dulu", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isImporting = true
                    coroutineScope.launch {
                        val success = onImportPayload(importPayloadText, passphrase)
                        isImporting = false
                        if (success) {
                            Toast.makeText(context, "Sinkronisasi Berhasil! Riwayat & Bookmark Didekripsi.", Toast.LENGTH_LONG).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Gagal Mengimpor! Kata sandi salah atau kode rusak.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("import_sync_code_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                if (isImporting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Dekripsi & Gabungkan Riwayat", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
