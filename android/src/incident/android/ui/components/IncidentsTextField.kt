package incident.android.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import incident.android.ui.theme.*

@Composable
fun IncidentsTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    isError: Boolean = false,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        singleLine = singleLine,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        isError = isError,
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = ContainerColor,
            unfocusedContainerColor = ContainerColor,
            disabledContainerColor = ContainerColor,
            errorContainerColor = ErrorContainerColor,
            focusedIndicatorColor = PrimaryColor,
            unfocusedIndicatorColor = BorderColor,
            errorIndicatorColor = ErrorColor,
            focusedTextColor = TextColor,
            unfocusedTextColor = TextColor,
            cursorColor = PrimaryColor,
            focusedPlaceholderColor = PlaceholderColor,
            unfocusedPlaceholderColor = PlaceholderColor
        )
    )
}