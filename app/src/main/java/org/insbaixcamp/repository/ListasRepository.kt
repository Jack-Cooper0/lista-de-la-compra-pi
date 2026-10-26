package org.insbaixcamp.repository

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
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

    private val databaseUrl = "https://lista-de-la-compra-2d920-default-rtdb.europe-west1.firebasedatabase.app"
    private val dbRef = Firebase.database(databaseUrl).getReference("lists")

    private val _listas = MutableStateFlow<List<ListaCompra>>(emptyList())
    val listas: StateFlow<List<ListaCompra>> = _listas.asStateFlow()

    private var valueEventListener: ValueEventListener? = null

    init {
        _listas.value = emptyList()

        try {
            Firebase.database(databaseUrl).setPersistenceEnabled(true)
        } catch (_: Exception) {}

        dbRef.keepSynced(true)

        // Sincronizar escucha con el estado de autenticación de Firebase
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            if (auth.currentUser != null) {
                iniciarEscuchaFirebase()
            } else {
                iniciarEscuchaFirebase()
            }
        }

        // Iniciar la escucha inicial
        iniciarEscuchaFirebase()
    }

    private fun iniciarEscuchaFirebase() {
        detenerEscuchaFirebase()

        val user = FirebaseAuth.getInstance().currentUser
        val currentUid = user?.uid ?: ""

        val query = if (currentUid.isNotEmpty()) {
            dbRef.orderByChild("members/$currentUid").equalTo(true)
        } else {
            dbRef
        }

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val listasFirebase = mutableListOf<ListaCompra>()
                val currentEmail = user?.email ?: ""

                for (child in snapshot.children) {
                    val lista = child.getValue(ListaCompra::class.java)
                    if (lista != null) {
                        val esMiembro = currentUid.isNotEmpty() && lista.members.containsKey(currentUid)
                        val esPropietario = currentEmail.isNotEmpty() && (lista.propietarioEmail == currentEmail || lista.usuariosConAcceso.contains(currentEmail))
                        val esSinDueno = lista.members.isEmpty() || lista.propietarioEmail.isBlank()

                        if (esMiembro || esPropietario || esSinDueno) {
                            listasFirebase.add(lista)
                        }
                    }
                }
                _listas.value = listasFirebase
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ListasRepository", "Error al leer de Firebase Database: ${error.message}", error.toException())
            }
        }

        query.addValueEventListener(valueEventListener!!)
    }

    private fun detenerEscuchaFirebase() {
        valueEventListener?.let {
            dbRef.removeEventListener(it)
            valueEventListener = null
        }
    }

    private fun guardarEnFirebase(lista: ListaCompra) {
        try {
            val user = FirebaseAuth.getInstance().currentUser
            val currentUid = user?.uid
            val currentEmail = user?.email

            var listaParaGuardar = lista

            if (currentUid != null && !listaParaGuardar.members.containsKey(currentUid)) {
                val nuevosMembers = listaParaGuardar.members.toMutableMap()
                nuevosMembers[currentUid] = true
                listaParaGuardar = listaParaGuardar.copy(members = nuevosMembers)
            }

            if (currentEmail != null && (listaParaGuardar.propietarioEmail.isBlank() || listaParaGuardar.propietarioEmail == "alumne@insbaixcamp.cat")) {
                val nuevosUsuarios = (listaParaGuardar.usuariosConAcceso + currentEmail).distinct()
                listaParaGuardar = listaParaGuardar.copy(
                    propietarioEmail = currentEmail,
                    usuariosConAcceso = nuevosUsuarios
                )
            }

            dbRef.child(listaParaGuardar.id).setValue(listaParaGuardar) { error, _ ->
                if (error != null) {
                    Log.e("ListasRepository", "Error al guardar en Firebase: ${error.message}")
                } else {
                    Log.d("ListasRepository", "Lista guardada con éxito en Firebase: ${listaParaGuardar.nombre}")
                }
            }
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
        val user = FirebaseAuth.getInstance().currentUser
        val currentUid = user?.uid ?: ""
        val emailFinal = user?.email?.takeIf { it.isNotBlank() } ?: propietarioEmail

        val membersMap = if (currentUid.isNotEmpty()) mapOf(currentUid to true) else emptyMap()

        val nuevaLista = ListaCompra(
            id = UUID.randomUUID().toString(),
            nombre = nombre,
            propietarioEmail = emailFinal,
            esCompartida = false,
            usuariosConAcceso = listOf(emailFinal),
            members = membersMap,
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
}
