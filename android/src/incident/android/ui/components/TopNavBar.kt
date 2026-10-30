package incident.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import incident.android.ui.icons.ArrowBackIcon

@Composable
fun TopNavBar(
    title: String,
    showBackButton: Boolean = true,
    onBackClick: (() -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    textColor: Color = Color.Black,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
            .height(50.dp)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (showBackButton && onBackClick != null) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(25.dp)
                    .align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = ArrowBackIcon,
                    contentDescription = "Back",
                    tint = textColor,
                    modifier = Modifier.size(25.dp)
                )
            }
        }

        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

