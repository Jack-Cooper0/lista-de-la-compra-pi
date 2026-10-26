package org.insbaixcamp

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.insbaixcamp.model.Categoria
import org.insbaixcamp.model.Producto
import org.insbaixcamp.ui.theme.ListaDeLaCompraTheme
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

    Scaffold(
        floatingActionButton = {
            if (listaActual != null) {
                ExtendedFloatingActionButton(
                    onClick = { mostrarDialogoNuevoProducto = true },
                    icon = { Text("+", fontSize = 22.sp, fontWeight = FontWeight.Bold) },
                    text = { Text("Afegir Producte", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(20.dp),
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 6.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // --- HEADER & LIST SELECTOR ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "🛒 La Meva Compra",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Organitza i comparteix",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                var dropdownExpandido by remember { mutableStateOf(false) }

                Box {
                    FilledTonalButton(
                        onClick = { dropdownExpandido = true },
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical =10.dp)
                    ) {
                        Text(
                            text = listaActual?.nombre?.take(15) ?: "Triar llista",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("▼", fontSize = 10.sp)
                    }

                    DropdownMenu(
                        expanded = dropdownExpandido,
                        onDismissRequest = { dropdownExpandido = false },
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        val misListas = listas.filter { it.propietarioEmail == emailUsuario || it.propietarioEmail.isBlank() }
                        val listasCompartidas = listas.filter { it.propietarioEmail != emailUsuario && it.propietarioEmail.isNotBlank() }

                        if (misListas.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("📍 LES MEVES LLISTES", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary) },
                                onClick = {},
                                enabled = false
                            )
                            misListas.forEach { llista ->
                                DropdownMenuItem(
                                    text = { Text(llista.nombre, fontWeight = if (llista.id == listaSeleccionadaId) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        viewModel.seleccionarLista(llista.id)
                                        dropdownExpandido = false
                                    }
                                )
                            }
                        }

                        if (listasCompartidas.isNotEmpty()) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            DropdownMenuItem(
                                text = { Text("👥 COMPARTIDES", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary) },
                                onClick = {},
                                enabled = false
                            )
                            listasCompartidas.forEach { llista ->
                                DropdownMenuItem(
                                    text = { Text("${llista.nombre} (${llista.propietarioEmail})") },
                                    onClick = {
                                        viewModel.seleccionarLista(llista.id)
                                        dropdownExpandido = false
                                    }
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        DropdownMenuItem(
                            text = { Text("+ Crear nova llista", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold) },
                            onClick = {
                                dropdownExpandido = false
                                mostrarDialogoNuevaLista = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- LIST DETAILS & ACTIONS CARD ---
            if (listaActual != null) {
                val totalProds = listaActual.productos.size
                val completadosProds = listaActual.productos.count { it.completado }
                val progreso = if (totalProds > 0) completadosProds.toFloat() / totalProds else 0f

                ElevatedCard(
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(35.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = listaActual.nombre,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                tonalElevation = 1.dp
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    IconButton(onClick = { mostrarDialogoCompartir = true }, modifier = Modifier.size(36.dp)) {
                                        Text("🔗", fontSize = 14.sp)
                                    }
                                    IconButton(onClick = { mostrarDialogoEditarLista = true }, modifier = Modifier.size(36.dp)) {
                                        Text("✎", fontSize = 14.sp)
                                    }
                                    IconButton(onClick = { viewModel.eliminarLista(listaActual.id) }, modifier = Modifier.size(36.dp)) {
                                        Text("🗑", fontSize = 14.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress bar
                        LinearProgressIndicator(
                            progress = { progreso },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surface,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val esPropietari = listaActual.propietarioEmail == emailUsuario || listaActual.propietarioEmail.isBlank()
                            Text(
                                text = if (listaActual.esCompartida) "👥 Compartida (${listaActual.usuariosConAcceso.size})" else if (esPropietari) "👤 Personal" else "👥 ${listaActual.propietarioEmail}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$completadosProds de $totalProds completats",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- CATEGORIES BAR ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Categories",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { mostrarDialogoNuevaCategoria = true }, contentPadding = PaddingValues(4.dp)) {
                        Text("+ Afegir categoria", fontSize = 12.sp)
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
                            label = { Text("Totes") },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                    items(listaActual.categorias) { cat ->
                        FilterChip(
                            selected = categoriaFiltroId == cat.id,
                            onClick = { categoriaFiltroId = cat.id },
                            label = { Text(cat.nombre) },
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                Text(
                                    text = "✎",
                                    modifier = Modifier
                                        .clickable { categoriaAEditar = cat }
                                        .padding(horizontal = 2.dp),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- PRODUCTS LIST ---
                Text(
                    text = "Productes",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

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
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("✨", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aquesta categoria està buida",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 72.dp)
                    ) {
                        items(productosFiltrados, key = { it.id }) { prod ->
                            val categoria = listaActual.categorias.find { it.id == prod.categoriaId }
                            ItemProducto(
                                producto = prod,
                                nombreCategoria = categoria?.nombre ?: "General",
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
            } else {
                // --- EMPTY STATE (NO LISTS) ---
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    ElevatedCard(
                        shape = RoundedCornerShape(28.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text("🛒", fontSize = 56.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Comença creant una llista",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Organitza les teves compres setmanals i comparteix-les amb qui vulguis.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { mostrarDialogoNuevaLista = true },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("+ Crear nova llista")
                            }
                        }
                    }
                }
            }
        }
    }

    // --- DIÀLEGS MODALS ---

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

    if (mostrarDialogoEditarLista && listaActual != null) {
        DialogoTextoInput(
            titulo = "Editar Nom",
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

    if (mostrarDialogoCompartir && listaActual != null) {
        DialogoTextoInput(
            titulo = "Compartir Llista",
            etiqueta = "Email de l'usuari",
            textoBoton = "Compartir",
            onDismiss = { mostrarDialogoCompartir = false },
            onConfirmar = { email ->
                viewModel.compartirLista(listaActual.id, email)
                mostrarDialogoCompartir = false
            }
        )
    }

    if (mostrarDialogoNuevaCategoria && listaActual != null) {
        DialogoTextoInput(
            titulo = "Nova Categoria",
            etiqueta = "Nom de la categoria",
            textoBoton = "Afegir",
            onDismiss = { mostrarDialogoNuevaCategoria = false },
            onConfirmar = { nomCat ->
                viewModel.crearCategoria(listaActual.id, nomCat)
                mostrarDialogoNuevaCategoria = false
            }
        )
    }

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
    val cardAlpha by animateFloatAsState(if (producto.completado) 0.5f else 1f, label = "alpha")
    val cardColor by animateColorAsState(
        if (producto.completado) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface,
        label = "color"
    )

    OutlinedCard(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = cardColor),
        border = BorderStroke(
            width = 1.dp,
            color = if (producto.completado) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(cardAlpha)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = producto.completado,
                onCheckedChange = { onToggleCompletado() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary
                )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = producto.nombre,
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp,
                    textDecoration = if (producto.completado) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (producto.completado) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 3.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = nombreCategoria,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    if (producto.cantidad.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Cant: ${producto.cantidad}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            IconButton(onClick = onEditar, modifier = Modifier.size(32.dp)) {
                Text("✎", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEliminar, modifier = Modifier.size(32.dp)) {
                Text("🗑", fontSize = 13.sp, color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
            }
        }
    }
}

// --- DIÀLEGS MODALS ---

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
        shape = RoundedCornerShape(28.dp),
        title = { Text(titulo, fontWeight = FontWeight.Bold) },
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
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (error) {
                    Text(
                        text = "Aquest camp no pot estar buit",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (texto.isBlank()) {
                        error = true
                    } else {
                        onConfirmar(texto)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
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
        shape = RoundedCornerShape(28.dp),
        title = { Text("Gestionar Categoria", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nom de la categoria") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
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
                Button(
                    onClick = {
                        if (nombre.isNotBlank()) onGuardar(nombre)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
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
        shape = RoundedCornerShape(28.dp),
        title = { Text(titulo, fontWeight = FontWeight.Bold) },
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
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (errorNombre) {
                    Text(
                        text = "El nom del producte és obligatori",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Start
                    )
                }

                OutlinedTextField(
                    value = cantidad,
                    onValueChange = { cantidad = it },
                    label = { Text("Quantitat / Nota (opcional, ej: 1 kg, 2L)") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Categoria:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categorias) { cat ->
                        FilterChip(
                            selected = categoriaIdSeleccionada == cat.id,
                            onClick = { categoriaIdSeleccionada = cat.id },
                            label = { Text(cat.nombre) },
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nombre.isBlank()) {
                        errorNombre = true
                    } else {
                        onConfirmar(nombre, categoriaIdSeleccionada, cantidad)
                    }
                },
                shape = RoundedCornerShape(12.dp)
            ) {
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

@Preview(showBackground = true)
@Composable
fun ListasPreview() {
    ListaDeLaCompraTheme {
        Greeting("Android")
    }
}
