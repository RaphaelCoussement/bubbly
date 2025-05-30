package org.raphou.bubbly.home

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import org.raphou.bubbly.home.R.string.choisis_ton_pseudo
import org.raphou.bubbly.home.R.string.pseudo_vide_ou_d_j_pris
import org.raphou.bubbly.ui.R
import org.raphou.bubbly.ui.R.*
import androidx.compose.material3.ExposedDropdownMenuDefaults.outlinedTextFieldColors

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("ResourceAsColor")
@Composable
fun ChoosePseudoScreen(navController: NavHostController) {
    val viewModel: ChoosePseudoScreenViewModel = viewModel()
    val pseudo by viewModel.pseudo.collectAsState()
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(id = color.beige_background))
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(choisis_ton_pseudo),
            style = MaterialTheme.typography.headlineMedium,
            color = colorResource(id = color.orange_primary)
        )

        Spacer(modifier = Modifier.height(20.dp))

        OutlinedTextField(
            value = pseudo,
            onValueChange = {
                viewModel.onPseudoChange(it)
                error = false
            },
            label = { Text(stringResource(org.raphou.bubbly.home.R.string.ton_pseudo)) },
            isError = error,
            singleLine = true,
            colors = outlinedTextFieldColors(
                focusedBorderColor = colorResource(id = color.orange_primary),
                unfocusedBorderColor = colorResource(id = color.orange_primary),
                cursorColor = colorResource(id = color.orange_primary),
                focusedLabelColor = colorResource(id = color.orange_primary),
                unfocusedLabelColor = colorResource(id = color.orange_primary),
            )
        )

        if (error) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(pseudo_vide_ou_d_j_pris),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = {
                scope.launch {
                    val success = viewModel.savePseudo()
                    if (success) {
                        navController.navigate("home") {
                            popUpTo("choosePseudo") { inclusive = true }
                        }
                    } else {
                        error = true
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = colorResource(id = R.color.orange_primary),
                contentColor = Color.White
            )
        ) {
            Text("Let's go ")
        }
    }
}
