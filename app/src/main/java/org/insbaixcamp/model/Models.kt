package org.insbaixcamp.model

/**
 * Representa un producto de la lista de la compra.
 */
data class Producto(
    var id: String = "",
    var nombre: String = "",
    var completado: Boolean = false,
    var categoriaId: String = "",
    var cantidad: String = ""
)

/**
 * Representa una categoría para clasificar productos (ej: Lláctics, Fruites, etc.).
 */
data class Categoria(
    var id: String = "",
    var nombre: String = ""
)

/**
 * Representa una lista de la compra que puede ser personal o compartida con otros usuarios.
 */
data class ListaCompra(
    var id: String = "",
    var nombre: String = "",
    var propietarioEmail: String = "",
    var esCompartida: Boolean = false,
    var usuariosConAcceso: List<String> = emptyList(),
    var categorias: List<Categoria> = emptyList(),
    var productos: List<Producto> = emptyList()
)
