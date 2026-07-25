package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BrowserSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuSheet(
    settings: BrowserSettings,
    blockedAdsCount: Int,
    dataSavedFormatted: String,
    onToggleAdBlock: () -> Unit,
    onToggleDataSaver: () -> Unit,
    onToggleTempImages: () -> Unit,
    onToggleTheme: () -> Unit,
    onToggleDesktop: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSync: () -> Unit,
    onDismiss: () -> Unit
) {
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
        ) {
            Text(
                text = "PENGATURAN NOIR",
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main Settings Grid (AdBlock, DataSaver, Theme, Desktop)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MenuFeatureCard(
                    title = "Pemblokir Iklan",
                    subtitle = if (settings.isAdBlockEnabled) "$blockedAdsCount Iklan" else "Nonaktif",
                    icon = Icons.Default.Shield,
                    isActive = settings.isAdBlockEnabled,
                    onClick = onToggleAdBlock,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("menu_adblock_toggle")
                )

                MenuFeatureCard(
                    title = "Mode Hemat Data",
                    subtitle = if (settings.isDataSaverEnabled) dataSavedFormatted else "Nonaktif",
                    icon = Icons.Default.DataSaverOn,
                    isActive = settings.isDataSaverEnabled,
                    onClick = onToggleDataSaver,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("menu_datasaver_toggle")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MenuFeatureCard(
                    title = "Tema Noir",
                    subtitle = if (settings.isDarkTheme) "Hitam Gelap" else "Putih Terang",
                    icon = if (settings.isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                    isActive = settings.isDarkTheme,
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("menu_theme_toggle")
                )

                MenuFeatureCard(
                    title = "Situs Desktop",
                    subtitle = if (settings.isDesktopMode) "Tampilan PC" else "Tampilan Seluler",
                    icon = Icons.Default.DesktopWindows,
                    isActive = settings.isDesktopMode,
                    onClick = onToggleDesktop,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("menu_desktop_toggle")
                )
            }

            if (settings.isDataSaverEnabled) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggleTempImages() },
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Muat Gambar Sementara",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (settings.allowImagesTemporarily) "Gambar diizinkan di halaman ini" else "Gambar diblokir untuk hemat data",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                        Switch(
                            checked = settings.allowImagesTemporarily,
                            onCheckedChange = { onToggleTempImages() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Divider(color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(16.dp))

            // Navigation Actions (History & Sync)
            MenuItemRow(
                title = "Riwayat & Bookmark",
                subtitle = "Kelola situs favorit dan riwayat penelusuran",
                icon = Icons.Default.History,
                onClick = {
                    onDismiss()
                    onOpenHistory()
                },
                modifier = Modifier.testTag("menu_history_button")
            )

            Spacer(modifier = Modifier.height(8.dp))

            MenuItemRow(
                title = "Sinkronisasi Aman Antar Perangkat",
                subtitle = "Enkripsi E2EE AES-256 untuk riwayat & bookmark",
                icon = Icons.Default.Sync,
                onClick = {
                    onDismiss()
                    onOpenSync()
                },
                modifier = Modifier.testTag("menu_sync_button")
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MenuFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(78.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    modifier = Modifier.size(18.dp),
                    tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isActive) "AKTIF" else "OFF",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 9.sp,
                    color = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

@Composable
fun MenuItemRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
