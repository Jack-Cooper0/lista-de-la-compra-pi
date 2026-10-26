package org.insbaixcamp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.insbaixcamp.model.Categoria
import org.insbaixcamp.model.Producto
import org.insbaixcamp.viewmodel.ListasViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListasScreen(
    viewModel: ListasViewModel = viewModel()
) {
    val listas by viewModel.listas.collectAsState()
    val listaSeleccionadaId by viewModel.listaSeleccionadaId.collectAsState()
    val emailUsuario = viewModel.obtenerEmailUsuarioActual()

    // Seleccionar la primera lista disponible si no hay ninguna seleccionada
    val listaActual = listas.find { it.id == listaSeleccionadaId } ?: listas.firstOrNull()

    // Estados para diálogos
    var mostrarDialogoNuevaLista by remember { mutableStateOf(false) }
    var mostrarDialogoEditarLista by remember { mutableStateOf(false) }
    var mostrarDialogoCompartir by remember { mutableStateOf(false) }
    var mostrarDialogoNuevaCategoria by remember { mutableStateOf(false) }
    var mostrarDialogoNuevoProducto by remember { mutableStateOf(false) }
    var productoAEditar by remember { mutableStateOf<Producto?>(null) }
    var categoriaAEditar by remember { mutableStateOf<Categoria?>(null) }

    // Estado para filtro de categoría seleccionada ("" = Totes)
    var categoriaFiltroId by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // --- SECTOR SUPERIOR: SELECCIÓ DE LLISTA I COMPARTIR ---
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Llistes de la Compra",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Selector de llista (Dropdown)
                var dropdownExpandido by remember { mutableStateOf(false) }

                Box {
                    OutlinedButton(
                        onClick = { dropdownExpandido = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = listaActual?.nombre ?: "Cap llista seleccionada",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Text("▼", fontSize = 12.sp)
                        }
                    }

                    DropdownMenu(
                        expanded = dropdownExpandido,
                        onDismissRequest = { dropdownExpandido = false }
                    ) {
                        val misListas = listas.filter { it.propietarioEmail == emailUsuario || it.propietarioEmail.isBlank() }
                        val listasCompartidas = listas.filter { it.propietarioEmail != emailUsuario && it.propietarioEmail.isNotBlank() }

                        if (misListas.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("--- LES MEVES LLISTES ---", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                onClick = {},
                                enabled = false
                            )
                            misListas.forEach { llista ->
                                DropdownMenuItem(
                                    text = { Text(llista.nombre) },
                                    onClick = {
                                        viewModel.seleccionarLista(llista.id)
                                        dropdownExpandido = false
                                    }
                                )
                            }
                        }

                        if (listasCompartidas.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("--- COMPARTIDES AMB MI ---", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                                onClick = {},
                                enabled = false
                            )
                            listasCompartidas.forEach { llista ->
                                DropdownMenuItem(
                                    text = { Text("${llista.nombre} (de ${llista.propietarioEmail})") },
                                    onClick = {
                                        viewModel.seleccionarLista(llista.id)
                                        dropdownExpandido = false
                                    }
                                )
                            }
                        }

                        HorizontalDivider()

                        DropdownMenuItem(
                            text = { Text("+ Crear nova llista", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                dropdownExpandido = false
                                mostrarDialogoNuevaLista = true
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botons d'acció per a la llista actual
                if (listaActual != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val esPropietari = listaActual.propietarioEmail == emailUsuario || listaActual.propietarioEmail.isBlank()
                        Text(
                            text = if (listaActual.esCompartida) "Compartida (${listaActual.usuariosConAcceso.size} usuaris)" else if (esPropietari) "Personal" else "Compartida per ${listaActual.propietarioEmail}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Row {
                            TextButton(onClick = { mostrarDialogoCompartir = true }) {
                                Text("Compartir")
                            }
                            TextButton(onClick = { mostrarDialogoEditarLista = true }) {
                                Text("Editar")
                            }
                            TextButton(
                                onClick = { viewModel.eliminarLista(listaActual.id) },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Eliminar")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (listaActual == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Button(onClick = { mostrarDialogoNuevaLista = true }) {
                    Text("Crear la teva primera llista de la compra")
                }
            }
        } else {
            // --- SECTOR CATEGORIES ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { mostrarDialogoNuevaCategoria = true }) {
                    Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = categoriaFiltroId == "",
                        onClick = { categoriaFiltroId = "" },
                        label = { Text("Totes") }
                    )
                }
                items(listaActual.categorias) { cat ->
                    FilterChip(
                        selected = categoriaFiltroId == cat.id,
                        onClick = { categoriaFiltroId = cat.id },
                        label = { Text(cat.nombre) },
                        trailingIcon = {
                            Text(
                                text = "✎",
                                modifier = Modifier
                                    .clickable { categoriaAEditar = cat }
                                    .padding(horizontal = 4.dp),
                                fontSize = 12.sp
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- SECTOR PRODUCTES ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Productes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = { mostrarDialogoNuevoProducto = true }
                ) {
                    Text("+ Nou Producte")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val productosFiltrados = if (categoriaFiltroId.isBlank()) {
                listaActual.productos
            } else {
                listaActual.productos.filter { it.categoriaId == categoriaFiltroId }
            }

            if (productosFiltrados.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No hi ha cap producte en aquesta llista o categoria.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(productosFiltrados) { prod ->
                        val categoria = listaActual.categorias.find { it.id == prod.categoriaId }
                        ItemProducto(
                            producto = prod,
                            nombreCategoria = categoria?.nombre ?: "Sense categoria",
                            onToggleCompletado = {
                                viewModel.toggleProductoCompletado(listaActual.id, prod.id)
                            },
                            onEditar = {
                                productoAEditar = prod
                            },
                            onEliminar = {
                                viewModel.eliminarProducto(listaActual.id, prod.id)
                            }
                        )
                    }
                }
            }
        }
    }

    // --- DIÀLEGS MODALS ---

    // 1. Crear Nueva Lista
    if (mostrarDialogoNuevaLista) {
        DialogoTextoInput(
            titulo = "Crear Nova Llista",
            etiqueta = "Nom de la llista",
            textoBoton = "Crear",
            onDismiss = { mostrarDialogoNuevaLista = false },
            onConfirmar = { nom ->
                viewModel.crearNuevaLista(nom)
                mostrarDialogoNuevaLista = false
            }
        )
    }

    // 2. Editar Nombre Lista
    if (mostrarDialogoEditarLista && listaActual != null) {
        DialogoTextoInput(
            titulo = "Editar Nom de la Llista",
            etiqueta = "Nou nom",
            textoInicial = listaActual.nombre,
            textoBoton = "Desar",
            onDismiss = { mostrarDialogoEditarLista = false },
            onConfirmar = { nouNom ->
                viewModel.editarNombreLista(listaActual.id, nouNom)
                mostrarDialogoEditarLista = false
            }
        )
    }

    // 3. Compartir Lista
    if (mostrarDialogoCompartir && listaActual != null) {
        DialogoTextoInput(
            titulo = "Compartir Llista",
            etiqueta = "Email de l'usuari amb qui compartir",
            textoBoton = "Compartir",
            onDismiss = { mostrarDialogoCompartir = false },
            onConfirmar = { email ->
                viewModel.compartirLista(listaActual.id, email)
                mostrarDialogoCompartir = false
            }
        )
    }

    // 4. Crear Categoria
    if (mostrarDialogoNuevaCategoria && listaActual != null) {
        DialogoTextoInput(
            titulo = "Afegir Categoria",
            etiqueta = "Nom de la categoria (obligatori)",
            textoBoton = "Afegir",
            onDismiss = { mostrarDialogoNuevaCategoria = false },
            onConfirmar = { nomCat ->
                viewModel.crearCategoria(listaActual.id, nomCat)
                mostrarDialogoNuevaCategoria = false
            }
        )
    }

    // 5. Editar Categoria
    if (categoriaAEditar != null && listaActual != null) {
        val cat = categoriaAEditar!!
        DialogoEditarEliminarCategoria(
            categoria = cat,
            onDismiss = { categoriaAEditar = null },
            onGuardar = { nouNom ->
                viewModel.editarCategoria(listaActual.id, cat.id, nouNom)
                categoriaAEditar = null
            },
            onEliminar = {
                viewModel.eliminarCategoria(listaActual.id, cat.id)
                categoriaAEditar = null
            }
        )
    }

    // 6. Crear Producte
    if (mostrarDialogoNuevoProducto && listaActual != null) {
        DialogoFormularioProducto(
            titulo = "Nou Producte",
            categorias = listaActual.categorias,
            categoriaDefectoId = if (categoriaFiltroId.isNotBlank()) categoriaFiltroId else listaActual.categorias.firstOrNull()?.id ?: "",
            onDismiss = { mostrarDialogoNuevoProducto = false },
            onConfirmar = { nom, catId, cant ->
                viewModel.crearProducto(listaActual.id, nom, catId, cant)
                mostrarDialogoNuevoProducto = false
            }
        )
    }

    // 7. Editar Producte
    if (productoAEditar != null && listaActual != null) {
        val prod = productoAEditar!!
        DialogoFormularioProducto(
            titulo = "Editar Producte",
            nombreInicial = prod.nombre,
            cantidadInicial = prod.cantidad,
            categoriaDefectoId = prod.categoriaId,
            categorias = listaActual.categorias,
            onDismiss = { productoAEditar = null },
            onConfirmar = { nom, catId, cant ->
                viewModel.editarProducto(listaActual.id, prod.id, nom, catId, cant)
                productoAEditar = null
            }
        )
    }
}

@Composable
fun ItemProducto(
    producto: Producto,
    nombreCategoria: String,
    onToggleCompletado: () -> Unit,
    onEditar: () -> Unit,
    onEliminar: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (producto.completado) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = producto.completado,
                onCheckedChange = { onToggleCompletado() }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = producto.nombre,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    textDecoration = if (producto.completado) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (producto.completado) Color.Gray else Color.Unspecified
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = nombreCategoria,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )

                    if (producto.cantidad.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Cant: ${producto.cantidad}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(onClick = onEditar) {
                Text("✎", fontSize = 16.sp)
            }
            IconButton(onClick = onEliminar) {
                Text("🗑", fontSize = 16.sp, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// --- COMPONENTES AUXILIARES DE DIÁLOGOS ---

@Composable
fun DialogoTextoInput(
    titulo: String,
    etiqueta: String,
    textoInicial: String = "",
    textoBoton: String = "Acceptar",
    onDismiss: () -> Unit,
    onConfirmar: (String) -> Unit
) {
    var texto by remember { mutableStateOf(textoInicial) }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            Column {
                OutlinedTextField(
                    value = texto,
                    onValueChange = {
                        texto = it
                        if (it.isNotBlank()) error = false
                    },
                    label = { Text(etiqueta) },
                    isError = error,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (error) {
                    Text(
                        text = "Aquest camp no pot estar buit",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (texto.isBlank()) {
                    error = true
                } else {
                    onConfirmar(texto)
                }
            }) {
                Text(textoBoton)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun DialogoEditarEliminarCategoria(
    categoria: Categoria,
    onDismiss: () -> Unit,
    onGuardar: (String) -> Unit,
    onEliminar: () -> Unit
) {
    var nombre by remember { mutableStateOf(categoria.nombre) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gestionar Categoria") },
        text = {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nom de la categoria") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Row {
                TextButton(
                    onClick = onEliminar,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    if (nombre.isNotBlank()) onGuardar(nombre)
                }) {
                    Text("Desar")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogoFormularioProducto(
    titulo: String,
    nombreInicial: String = "",
    cantidadInicial: String = "",
    categoriaDefectoId: String = "",
    categorias: List<Categoria>,
    onDismiss: () -> Unit,
    onConfirmar: (nombre: String, categoriaId: String, cantidad: String) -> Unit
) {
    var nombre by remember { mutableStateOf(nombreInicial) }
    var cantidad by remember { mutableStateOf(cantidadInicial) }
    var categoriaIdSeleccionada by remember {
        mutableStateOf(if (categoriaDefectoId.isNotBlank()) categoriaDefectoId else categorias.firstOrNull()?.id ?: "")
    }
    var errorNombre by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titulo) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = {
                        nombre = it
                        if (it.isNotBlank()) errorNombre = false
                    },
                    label = { Text("Nom del producte (obligatori)") },
                    isError = errorNombre,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorNombre) {
                    Text(
                        text = "El nom del producte és obligatori",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = cantidad,
                    onValueChange = { cantidad = it },
                    label = { Text("Quantitat / Nota (opcional, ej: 1 kg, 2L)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Categoria:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categorias) { cat ->
                        FilterChip(
                            selected = categoriaIdSeleccionada == cat.id,
                            onClick = { categoriaIdSeleccionada = cat.id },
                            label = { Text(cat.nombre) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (nombre.isBlank()) {
                    errorNombre = true
                } else {
                    onConfirmar(nombre, categoriaIdSeleccionada, cantidad)
                }
            }) {
                Text("Desar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
