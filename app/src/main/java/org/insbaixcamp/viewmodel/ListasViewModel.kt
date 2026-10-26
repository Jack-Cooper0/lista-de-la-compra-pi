package org.insbaixcamp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.insbaixcamp.model.ListaCompra
import org.insbaixcamp.repository.ListasRepository

/**
 * ViewModel per a la pantalla principal de llistes de la compra.
 * S'encarrega de coordinar l'estat de la UI amb el repositori.
 */
class ListasViewModel(
    application: Application
) : AndroidViewModel(application) {

    val repository: ListasRepository = ListasRepository(application.applicationContext)

    val listas: StateFlow<List<ListaCompra>> = repository.listas

    private val _listaSeleccionadaId = MutableStateFlow<String?>(null)
    val listaSeleccionadaId: StateFlow<String?> = _listaSeleccionadaId.asStateFlow()

    // Email de l'usuari actual (de Firebase Auth o usuari per defecte)
    fun obtenerEmailUsuarioActual(): String {
        val user = FirebaseAuth.getInstance().currentUser
        return user?.email ?: "alumne@insbaixcamp.cat"
    }

    fun seleccionarLista(id: String) {
        _listaSeleccionadaId.value = id
    }

    fun crearNuevaLista(nombre: String) {
        if (nombre.isBlank()) return
        val email = obtenerEmailUsuarioActual()
        val nuevaLista = repository.crearLista(nombre = nombre, propietarioEmail = email)
        _listaSeleccionadaId.value = nuevaLista.id
    }

    fun editarNombreLista(listaId: String, nuevoNombre: String) {
        repository.editarNombreLista(listaId, nuevoNombre)
    }

    fun compartirLista(listaId: String, emailDestinatario: String) {
        repository.compartirLista(listaId, emailDestinatario)
    }

    fun eliminarLista(listaId: String) {
        repository.eliminarLista(listaId)
        if (_listaSeleccionadaId.value == listaId) {
            _listaSeleccionadaId.value = null
        }
    }

    // --- CATEGORIES ---

    fun crearCategoria(listaId: String, nombreCategoria: String) {
        repository.crearCategoria(listaId, nombreCategoria)
    }

    fun editarCategoria(listaId: String, categoriaId: String, nuevoNombre: String) {
        repository.editarCategoria(listaId, categoriaId, nuevoNombre)
    }

    fun eliminarCategoria(listaId: String, categoriaId: String) {
        repository.eliminarCategoria(listaId, categoriaId)
    }

    // --- PRODUCTES ---

    fun crearProducto(listaId: String, nombre: String, categoriaId: String, cantidad: String) {
        repository.crearProducto(listaId, nombre, categoriaId, cantidad)
    }

    fun editarProducto(listaId: String, productoId: String, nuevoNombre: String, nuevaCategoriaId: String, nuevaCantidad: String) {
        repository.editarProducto(listaId, productoId, nuevoNombre, nuevaCategoriaId, nuevaCantidad)
    }

    fun toggleProductoCompletado(listaId: String, productoId: String) {
        repository.toggleProductoCompletado(listaId, productoId)
    }

    fun eliminarProducto(listaId: String, productoId: String) {
        repository.eliminarProducto(listaId, productoId)
    }
}
