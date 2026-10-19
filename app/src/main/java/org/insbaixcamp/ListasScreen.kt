package org.insbaixcamp

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class Producto(val nombre: String, val completado: Boolean = false)

@Composable
fun ListasScreen() {
    // Estado: la lista de productos y el texto que se está escribiendo
    var productos by remember { mutableStateOf(listOf<Producto>()) }
    var textoNuevo by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Mi lista de la compra", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        // Fila con el campo de texto y el botón de añadir
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = textoNuevo,
                onValueChange = { textoNuevo = it },
                label = { Text("Producto") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                if (textoNuevo.isNotBlank()) {
                    productos = productos + Producto(textoNuevo)
                    textoNuevo = ""
                }
            }) { Text("Añadir") }
        }

        // La lista de productos, cada uno con su casilla
        LazyColumn {
            items(productos) { producto ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = producto.completado,
                        onCheckedChange = { marcado ->
                            productos = productos.map {
                                if (it == producto) it.copy(completado = marcado) else it
                            }
                        }
                    )
                    Text(producto.nombre)
                }
            }
        }
    }
}