package com.punto.app.presentation.components

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization

/**
 * Campo de texto que guarda su valor en un [MutableState] LOCAL.
 *
 * Por qué existe: si el texto viaja por ViewModel → StateFlow → collectAsState en cada tecla,
 * el valor llega al campo con un salto asíncrono (el teclado se siente lento y a veces se
 * "come" letras) y además se recompone toda la pantalla. Aquí el valor se lee dentro de esta
 * función, así que cada tecla solo recompone ESTE campo. El ViewModel recibe el texto una sola
 * vez, al guardar.
 */
@Composable
fun StateOutlinedTextField(
    state: MutableState<String>,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
) {
    OutlinedTextField(
        value = state.value,
        onValueChange = { state.value = it },
        label = { Text(label) },
        modifier = modifier,
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = if (singleLine) ImeAction.Next else ImeAction.Default,
        ),
    )
}
