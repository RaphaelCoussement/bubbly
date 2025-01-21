package org.raphou.bubbly.home

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen() {

    Text(
        text = "Bonjour,",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp)
    )

    Text(
        text = "A tous,",
        fontSize = 28.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 16.dp)
    )
}
