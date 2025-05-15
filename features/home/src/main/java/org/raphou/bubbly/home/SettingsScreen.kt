package org.raphou.bubbly.home

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import org.raphou.bubbly.domain.home.LanguageManager
import org.raphou.bubbly.home.R.string.*
import org.raphou.bubbly.ui.R.*
import java.util.Locale

@Composable
fun SettingsScreen(navController: NavHostController) {
    val context = LocalContext.current
    var currentLocale by remember { mutableStateOf(LanguageManager.getSavedLanguage(context)) }
    var expanded by remember { mutableStateOf(false) }

    val languages = listOf("fr", "en")
    val languageLabels = mapOf(
        "fr" to stringResource(R.string.fran_ais),
        "en" to stringResource(R.string.anglais)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = color.beige_background))
            .padding(16.dp)
    ) {
        // Bouton retour
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.retour),
                    tint = colorResource(id = color.orange_primary)
                )
            }
            Text(
                text = stringResource(R.string.param_tres),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Incitation à noter l'app
        Card(
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.vous_aimez_bubbly),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.laissez_une_note_ou_un_commentaire),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = {
                        val url = "https://play.google.com/store/apps/details?id=org.raphou.bubbly"
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colorResource(id = color.orange_primary))
                ) {
                    Text(text = stringResource(noter_l_application))
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Version : 1.0.0",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.TopStart)
                .background(Color.White, shape = RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = stringResource(R.string.changer_la_langue),
                    tint = colorResource(id = color.orange_primary),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = stringResource(R.string.langue, languageLabels[currentLocale] ?: ""),
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color.White)
            ) {
                languages.forEach { locale ->
                    val isSelected = locale == currentLocale
                    val backgroundColor = if (isSelected) colorResource(id = color.orange_primary) else Color.White
                    val textColor = if (isSelected) Color.White else Color.Black

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = languageLabels[locale] ?: locale,
                                color = textColor
                            )
                        },
                        onClick = {
                            currentLocale = locale
                            LanguageManager.saveLanguage(context, locale)
                            LanguageManager.applyLanguage(context, locale)
                            LanguageManager.restartActivity(context as Activity)
                            expanded = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(backgroundColor)
                    )
                }
            }
        }
    }
}
