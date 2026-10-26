package org.insbaixcamp.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.database
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.insbaixcamp.model.Categoria
import org.insbaixcamp.model.ListaCompra
import org.insbaixcamp.model.Producto
import java.util.UUID

/**
 * Repositori encarregat de gestionar les llistes de la compra.
 * Està preparat per treballar en memòria local i sincronitzar-se en temps real
 * amb Firebase Realtime Database.
 */
class ListasRepository {

    private val dbRef = Firebase.database.getReference("listas_compra")

    private val _listas = MutableStateFlow<List<ListaCompra>>(emptyList())
    val listas: StateFlow<List<ListaCompra>> = _listas.asStateFlow()

    init {
        // Cargar datos de muestra por defecto si no hay nada en local
        _listas.value = obtenerListasMuestra()

        // Sincronización en tiempo real con Firebase Realtime Database
        iniciarEscuchaFirebase()
    }

    private fun iniciarEscuchaFirebase() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listasFirebase = mutableListOf<ListaCompra>()
                for (child in snapshot.children) {
                    val lista = child.getValue(ListaCompra::class.java)
                    if (lista != null) {
                        listasFirebase.add(lista)
                    }
                }
                if (listasFirebase.isNotEmpty()) {
                    _listas.value = listasFirebase
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("ListasRepository", "Error al llegir de Firebase Database", error.toException())
            }
        })
    }

    private fun guardarEnFirebase(lista: ListaCompra) {
        try {
            dbRef.child(lista.id).setValue(lista)
        } catch (e: Exception) {
            Log.e("ListasRepository", "No s'ha pogut desar a Firebase", e)
        }
    }

    private fun eliminarDeFirebase(listaId: String) {
        try {
            dbRef.child(listaId).removeValue()
        } catch (e: Exception) {
            Log.e("ListasRepository", "No s'ha pogut eliminar de Firebase", e)
        }
    }

    private fun actualizarLista(listaActualizada: ListaCompra) {
        _listas.value = _listas.value.map {
            if (it.id == listaActualizada.id) listaActualizada else it
        }
        guardarEnFirebase(listaActualizada)
    }

    // --- GESTIÓ DE LLISTES ---

    fun crearLista(nombre: String, propietarioEmail: String): ListaCompra {
        val nuevaLista = ListaCompra(
            id = UUID.randomUUID().toString(),
            nombre = nombre,
            propietarioEmail = propietarioEmail,
            esCompartida = false,
            usuariosConAcceso = listOf(propietarioEmail),
            categorias = listOf(
                Categoria(id = UUID.randomUUID().toString(), nombre = "General")
            ),
            productos = emptyList()
        )
        _listas.value = _listas.value + nuevaLista
        guardarEnFirebase(nuevaLista)
        return nuevaLista
    }

    fun editarNombreLista(listaId: String, nuevoNombre: String) {
        val lista = _listas.value.find { it.id == listaId } ?: return
        val listaActualizada = lista.copy(nombre = nuevoNombre)
        actualizarLista(listaActualizada)
    }

    fun compartirLista(listaId: String, emailNuevoUsuario: String) {
        val lista = _listas.value.find { it.id == listaId } ?: return
        if (emailNuevoUsuario.isBlank()) return

        val nuevosUsuarios = (lista.usuariosConAcceso + emailNuevoUsuario).distinct()
        val listaActualizada = lista.copy(
            esCompartida = true,
            usuariosConAcceso = nuevosUsuarios
        )
        actualizarLista(listaActualizada)
    }

    fun eliminarLista(listaId: String) {
        _listas.value = _listas.value.filterNot { it.id == listaId }
        eliminarDeFirebase(listaId)
    }

    // --- GESTIÓ DE CATEGORIES ---

    fun crearCategoria(listaId: String, nombreCategoria: String) {
        if (nombreCategoria.isBlank()) return
        val lista = _listas.value.find { it.id == listaId } ?: return

        val nuevaCategoria = Categoria(
            id = UUID.randomUUID().toString(),
            nombre = nombreCategoria
        )
        val listaActualizada = lista.copy(
            categorias = lista.categorias + nuevaCategoria
        )
        actualizarLista(listaActualizada)
    }

    fun editarCategoria(listaId: String, categoriaId: String, nuevoNombre: String) {
        if (nuevoNombre.isBlank()) return
        val lista = _listas.value.find { it.id == listaId } ?: return

        val categoriasActualizadas = lista.categorias.map {
            if (it.id == categoriaId) it.copy(nombre = nuevoNombre) else it
        }
        val listaActualizada = lista.copy(categorias = categoriasActualizadas)
        actualizarLista(listaActualizada)
    }

    fun eliminarCategoria(listaId: String, categoriaId: String) {
        val lista = _listas.value.find { it.id == listaId } ?: return

        // Eliminar categoria y mover sus productos a la primera categoria disponible o dejarlos sin categoria
        val categoriasActualizadas = lista.categorias.filterNot { it.id == categoriaId }
        val fallbackCategoriaId = categoriasActualizadas.firstOrNull()?.id ?: ""

        val productosActualizados = lista.productos.map { prod ->
            if (prod.categoriaId == categoriaId) prod.copy(categoriaId = fallbackCategoriaId) else prod
        }

        val listaActualizada = lista.copy(
            categorias = categoriasActualizadas,
            productos = productosActualizados
        )
        actualizarLista(listaActualizada)
    }

    // --- GESTIÓ DE PRODUCTES ---

    fun crearProducto(listaId: String, nombre: String, categoriaId: String, cantidad: String) {
        if (nombre.isBlank()) return
        val lista = _listas.value.find { it.id == listaId } ?: return

        val nuevoProducto = Producto(
            id = UUID.randomUUID().toString(),
            nombre = nombre,
            completado = false,
            categoriaId = categoriaId,
            cantidad = cantidad
        )
        val listaActualizada = lista.copy(
            productos = lista.productos + nuevoProducto
        )
        actualizarLista(listaActualizada)
    }

    fun editarProducto(listaId: String, productoId: String, nuevoNombre: String, nuevaCategoriaId: String, nuevaCantidad: String) {
        if (nuevoNombre.isBlank()) return
        val lista = _listas.value.find { it.id == listaId } ?: return

        val productosActualizados = lista.productos.map {
            if (it.id == productoId) {
                it.copy(
                    nombre = nuevoNombre,
                    categoriaId = nuevaCategoriaId,
                    cantidad = nuevaCantidad
                )
            } else it
        }
        val listaActualizada = lista.copy(productos = productosActualizados)
        actualizarLista(listaActualizada)
    }

    fun toggleProductoCompletado(listaId: String, productoId: String) {
        val lista = _listas.value.find { it.id == listaId } ?: return

        val productosActualizados = lista.productos.map {
            if (it.id == productoId) it.copy(completado = !it.completado) else it
        }
        val listaActualizada = lista.copy(productos = productosActualizados)
        actualizarLista(listaActualizada)
    }

    fun eliminarProducto(listaId: String, productoId: String) {
        val lista = _listas.value.find { it.id == listaId } ?: return

        val productosActualizados = lista.productos.filterNot { it.id == productoId }
        val listaActualizada = lista.copy(productos = productosActualizados)
        actualizarLista(listaActualizada)
    }

    // --- DADES DE MOSTRA INICIALS ---

    private fun obtenerListasMuestra(): List<ListaCompra> {
        val catLlactics = Categoria(id = "cat_1", nombre = "Llàctics i Ou")
        val catFruita = Categoria(id = "cat_2", nombre = "Fruites i Verdures")
        val catBegudes = Categoria(id = "cat_3", nombre = "Begudes")

        val prod1 = Producto(id = "p1", nombre = "Llet de civada", completado = false, categoriaId = catLlactics.id, cantidad = "2L")
        val prod2 = Producto(id = "p2", nombre = "Formatge fresc", completado = true, categoriaId = catLlactics.id, cantidad = "200g")
        val prod3 = Producto(id = "p3", nombre = "Pomes vermelles", completado = false, categoriaId = catFruita.id, cantidad = "1 kg")
        val prod4 = Producto(id = "p4", nombre = "Aigua mineral", completado = false, categoriaId = catBegudes.id, cantidad = "1 pack")

        val listaPersonal = ListaCompra(
            id = "lista_1",
            nombre = "Compra setmanal casa",
            propietarioEmail = "alumne@insbaixcamp.cat",
            esCompartida = false,
            usuariosConAcceso = listOf("alumne@insbaixcamp.cat"),
            categorias = listOf(catLlactics, catFruita, catBegudes),
            productos = listOf(prod1, prod2, prod3, prod4)
        )

        val catFesta = Categoria(id = "cat_4", nombre = "Aperitius")
        val prodFesta1 = Producto(id = "p5", nombre = "Patates fregides", completado = false, categoriaId = catFesta.id, cantidad = "2 bosses")
        val prodFesta2 = Producto(id = "p6", nombre = "Refrescos", completado = true, categoriaId = catBegudes.id, cantidad = "6 llaunes")

        val listaCompartida = ListaCompra(
            id = "lista_2",
            nombre = "Festa del Projecte (Compartida)",
            propietarioEmail = "profe@insbaixcamp.cat",
            esCompartida = true,
            usuariosConAcceso = listOf("profe@insbaixcamp.cat", "alumne@insbaixcamp.cat"),
            categorias = listOf(catFesta, catBegudes),
            productos = listOf(prodFesta1, prodFesta2)
        )

        return listOf(listaPersonal, listaCompartida)
    }
}
