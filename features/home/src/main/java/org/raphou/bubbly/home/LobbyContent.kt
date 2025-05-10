package org.raphou.bubbly.home

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import org.raphou.bubbly.domain.lobby.Lobby
import org.raphou.bubbly.domain.lobby.Player
import org.raphou.bubbly.home.R.*
import org.raphou.bubbly.home.R.string.*
import org.raphou.bubbly.ui.R

@Composable
fun LobbyContent(lobby: Lobby?, onBack: () -> Unit, players: List<Player>) {
    val viewModel: LobbyContentViewModel = viewModel()
    val codeIsValid by viewModel.codeIsValid
    var showQRCode by remember { mutableStateOf(false) }
    val qrCodeSize by animateDpAsState(targetValue = if (showQRCode) 200.dp else 0.dp) // Animation du QR Code

    LaunchedEffect(lobby?.code) {
        lobby?.code?.let { viewModel.checkLobbyCode(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.beige_background))
            .padding(16.dp)
    ) {
        when (codeIsValid) {
            false -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = { onBack() },
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Retour",
                            tint = colorResource(id = R.color.orange_primary)
                        )
                    }

                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(code_incorrect),
                            color = colorResource(id = R.color.orange_primary),
                            style = MaterialTheme.typography.headlineMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            true -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    IconButton(
                        onClick = {
                            lobby?.code?.let { viewModel.deleteLobbyByCode(it) }
                            onBack()
                        },
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Retour",
                            tint = colorResource(id = R.color.orange_primary)
                        )
                    }

                    // Section Code de Session avec l'icône pour afficher/cacher le QR Code
                    Text(
                        text = stringResource(
                            code_de_sessionn,
                            lobby?.code ?: stringResource(chargement)
                        ),
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                        color = colorResource(id = R.color.orange_primary),
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .align(Alignment.CenterHorizontally)
                    )

                    // Icône QR Code en haut à droite
                    IconButton(
                        onClick = { showQRCode = !showQRCode },
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_qr_code_scanner_24),
                            contentDescription = stringResource(id = qr_code_scanner_icon),
                            tint = colorResource(id = R.color.orange_primary)
                        )
                    }

                    // Animation et affichage du QR Code
                    AnimatedVisibility(visible = showQRCode) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = generateQRCode("joinLobby/${lobby?.code}"),
                                contentDescription = "QR Code",
                                modifier = Modifier
                                    .size(qrCodeSize)
                                    .animateContentSize()
                            )
                        }
                    }

                    Text(
                        text = stringResource(participantss),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        if (players.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(aucun_participant),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.Gray,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            items(players) { player ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = MaterialTheme.shapes.medium,
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Text(
                                            text = player.name,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                            color = Color.Black,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colorResource(id = R.color.orange_primary))
                }
            }
        }
    }
}

fun generateQRCode(content: String, size: Int = 512): ImageBitmap {
    val bitMatrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    val width = bitMatrix.width
    val height = bitMatrix.height
    val pixels = IntArray(width * height) { i ->
        val x = i % width
        val y = i / width
        if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
    }
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap.asImageBitmap()
}

