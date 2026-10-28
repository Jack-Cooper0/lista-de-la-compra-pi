package org.insbaixcamp.repository

import android.content.Context
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
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Repositori encarregat de gestionar les llistes de la compra.
 * S'encarrega de la persistència en local (SharedPreferences/JSON) quan l'usuari no fa login,
 * i de la sincronització en temps real amb Firebase Realtime Database quan l'usuari inicia sessió.
 */
class ListasRepository(context: Context) {

    private val sharedPrefs = context.getSharedPreferences("listas_compra_local_prefs", Context.MODE_PRIVATE)

    private val databaseUrl = "https://lista-de-la-compra-2d920-default-rtdb.europe-west1.firebasedatabase.app"
    private val dbRef = Firebase.database(databaseUrl).getReference("lists")

    private val _listas = MutableStateFlow<List<ListaCompra>>(emptyList())
    val listas: StateFlow<List<ListaCompra>> = _listas.asStateFlow()

    private var valueEventListener: ValueEventListener? = null

    init {
        try {
            Firebase.database(databaseUrl).setPersistenceEnabled(true)
        } catch (_: Exception) {}

        dbRef.keepSynced(true)

        // 1. Carregar dades locals guardades prèviament en SharedPreferences
        val listasLocales = cargarDeLocal()
        if (listasLocales.isNotEmpty()) {
            _listas.value = listasLocales
        } else {
            val muestras = obtenerListasMuestra()
            _listas.value = muestras
            guardarEnLocal(muestras)
        }

        // 2. Escuchar cambios en la autenticación de Firebase
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            if (auth.currentUser != null) {
                iniciarEscuchaFirebase()
            } else {
                detenerEscuchaFirebase()
                val locales = cargarDeLocal()
                if (locales.isNotEmpty()) {
                    _listas.value = locales
                }
            }
        }

        if (FirebaseAuth.getInstance().currentUser != null) {
            iniciarEscuchaFirebase()
        }
    }

    // --- PERSISTÈNCIA EN LOCAL (SharedPreferences + JSON) ---

    private fun guardarEnLocal(listas: List<ListaCompra>) {
        try {
            val jsonArray = JSONArray()
            for (lista in listas) {
                jsonArray.put(listaToJson(lista))
            }
            sharedPrefs.edit().putString("listas_json", jsonArray.toString()).apply()
        } catch (e: Exception) {
            Log.e("ListasRepository", "Error al desar les llistes en local", e)
        }
    }

    private fun cargarDeLocal(): List<ListaCompra> {
        val jsonString = sharedPrefs.getString("listas_json", null) ?: return emptyList()
        val resultado = mutableListOf<ListaCompra>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                resultado.add(jsonToLista(obj))
            }
        } catch (e: Exception) {
            Log.e("ListasRepository", "Error al carregar les llistes de local", e)
        }
        return resultado
    }

    private fun listaToJson(lista: ListaCompra): JSONObject {
        val obj = JSONObject()
        obj.put("id", lista.id)
        obj.put("nombre", lista.nombre)
        obj.put("propietarioEmail", lista.propietarioEmail)
        obj.put("esCompartida", lista.esCompartida)

        val usuariosArr = JSONArray()
        lista.usuariosConAcceso.forEach { usuariosArr.put(it) }
        obj.put("usuariosConAcceso", usuariosArr)

        val membersObj = JSONObject()
        lista.members.forEach { (k, v) -> membersObj.put(k, v) }
        obj.put("members", membersObj)

        val catArr = JSONArray()
        lista.categorias.forEach { cat ->
            val catObj = JSONObject()
            catObj.put("id", cat.id)
            catObj.put("nombre", cat.nombre)
            catArr.put(catObj)
        }
        obj.put("categorias", catArr)

        val prodArr = JSONArray()
        lista.productos.forEach { prod ->
            val prodObj = JSONObject()
            prodObj.put("id", prod.id)
            prodObj.put("nombre", prod.nombre)
            prodObj.put("completado", prod.completado)
            prodObj.put("categoriaId", prod.categoriaId)
            prodObj.put("cantidad", prod.cantidad)
            prodArr.put(prodObj)
        }
        obj.put("productos", prodArr)

        return obj
    }

    private fun jsonToLista(obj: JSONObject): ListaCompra {
        val usuariosList = mutableListOf<String>()
        val usuariosArr = obj.optJSONArray("usuariosConAcceso")
        if (usuariosArr != null) {
            for (i in 0 until usuariosArr.length()) {
                usuariosList.add(usuariosArr.getString(i))
            }
        }

        val membersMap = mutableMapOf<String, Boolean>()
        val membersObj = obj.optJSONObject("members")
        if (membersObj != null) {
            val keys = membersObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                membersMap[key] = membersObj.optBoolean(key, true)
            }
        }

        val categoriasList = mutableListOf<Categoria>()
        val catArr = obj.optJSONArray("categorias")
        if (catArr != null) {
            for (i in 0 until catArr.length()) {
                val catObj = catArr.getJSONObject(i)
                categoriasList.add(
                    Categoria(
                        id = catObj.optString("id"),
                        nombre = catObj.optString("nombre")
                    )
                )
            }
        }

        val productosList = mutableListOf<Producto>()
        val prodArr = obj.optJSONArray("productos")
        if (prodArr != null) {
            for (i in 0 until prodArr.length()) {
                val prodObj = prodArr.getJSONObject(i)
                productosList.add(
                    Producto(
                        id = prodObj.optString("id"),
                        nombre = prodObj.optString("nombre"),
                        completado = prodObj.optBoolean("completado"),
                        categoriaId = prodObj.optString("categoriaId"),
                        cantidad = prodObj.optString("cantidad")
                    )
                )
            }
        }

        return ListaCompra(
            id = obj.optString("id"),
            nombre = obj.optString("nombre"),
            propietarioEmail = obj.optString("propietarioEmail"),
            esCompartida = obj.optBoolean("esCompartida"),
            usuariosConAcceso = usuariosList,
            members = membersMap,
            categorias = categoriasList,
            productos = productosList
        )
    }

    // --- FIREBASE REALTIME DATABASE ---

    private fun iniciarEscuchaFirebase() {
        detenerEscuchaFirebase()

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val user = FirebaseAuth.getInstance().currentUser
                val currentUid = user?.uid ?: ""
                val currentEmail = user?.email?.trim()?.lowercase() ?: ""

                val listasFirebase = mutableListOf<ListaCompra>()

                for (child in snapshot.children) {
                    val lista = child.getValue(ListaCompra::class.java)
                    if (lista != null) {
                        val propEmailLimpio = lista.propietarioEmail.trim().lowercase()
                        val usuariosAccesoLimpios = lista.usuariosConAcceso.map { it.trim().lowercase() }

                        val esMiembroUid = currentUid.isNotEmpty() && lista.members.containsKey(currentUid)
                        val esPropietarioOCompartida = currentEmail.isNotEmpty() && (
                            propEmailLimpio == currentEmail ||
                            usuariosAccesoLimpios.contains(currentEmail)
                        )
                        val esSinDueno = lista.members.isEmpty() || propEmailLimpio.isBlank()

                        if (esMiembroUid || esPropietarioOCompartida || esSinDueno) {
                            listasFirebase.add(lista)

                            if (currentUid.isNotEmpty() && !lista.members.containsKey(currentUid)) {
                                dbRef.child(lista.id).child("members").child(currentUid).setValue(true)
                            }
                        }
                    }
                }
                _listas.value = listasFirebase
                guardarEnLocal(listasFirebase)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ListasRepository", "Error al leer de Firebase Database: ${error.message}", error.toException())
            }
        }

        dbRef.addValueEventListener(valueEventListener!!)
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
            val currentEmail = user?.email?.trim()?.lowercase()

            var listaParaGuardar = lista

            if (currentUid != null && !listaParaGuardar.members.containsKey(currentUid)) {
                val nuevosMembers = listaParaGuardar.members.toMutableMap()
                nuevosMembers[currentUid] = true
                listaParaGuardar = listaParaGuardar.copy(members = nuevosMembers)
            }

            if (!currentEmail.isNullOrBlank()) {
                if (listaParaGuardar.propietarioEmail.isBlank() || listaParaGuardar.propietarioEmail == "alumne@insbaixcamp.cat") {
                    listaParaGuardar = listaParaGuardar.copy(propietarioEmail = currentEmail)
                }
                val usuariosLimpios = listaParaGuardar.usuariosConAcceso.map { it.trim().lowercase() }
                if (!usuariosLimpios.contains(currentEmail)) {
                    val nuevosUsuarios = (usuariosLimpios + currentEmail).distinct()
                    listaParaGuardar = listaParaGuardar.copy(usuariosConAcceso = nuevosUsuarios)
                }
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
        guardarEnLocal(_listas.value)
        guardarEnFirebase(listaActualizada)
    }

    // --- GESTIÓ DE LLISTES ---

    fun crearLista(nombre: String, propietarioEmail: String): ListaCompra {
        val user = FirebaseAuth.getInstance().currentUser
        val currentUid = user?.uid ?: ""
        val emailFinal = user?.email?.trim()?.lowercase()?.takeIf { it.isNotBlank() }
            ?: propietarioEmail.trim().lowercase()

        val membersMap = if (currentUid.isNotEmpty()) mapOf(currentUid to true) else emptyMap()

        val nuevaLista = ListaCompra(
            id = UUID.randomUUID().toString(),
            nombre = nombre,
            propietarioEmail = emailFinal,
            esCompartida = false,
            usuariosConAcceso = if (emailFinal.isNotBlank()) listOf(emailFinal) else emptyList(),
            members = membersMap,
            categorias = listOf(
                Categoria(id = UUID.randomUUID().toString(), nombre = "General")
            ),
            productos = emptyList()
        )
        _listas.value = _listas.value + nuevaLista
        guardarEnLocal(_listas.value)
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
        val emailLimpio = emailNuevoUsuario.trim().lowercase()
        if (emailLimpio.isBlank()) return

        val usuariosActualesLimpios = lista.usuariosConAcceso.map { it.trim().lowercase() }
        val nuevosUsuarios = (usuariosActualesLimpios + emailLimpio).distinct()
        val listaActualizada = lista.copy(
            esCompartida = true,
            usuariosConAcceso = nuevosUsuarios
        )
        actualizarLista(listaActualizada)
    }

    fun eliminarLista(listaId: String) {
        _listas.value = _listas.value.filterNot { it.id == listaId }
        guardarEnLocal(_listas.value)
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
