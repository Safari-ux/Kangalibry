package com.example.turkids.model

// reglas del registro sin android: se prueban con junit
class ValidadorUsuario {

    fun validarNombre(valor: String): String? =
        if (valor.isBlank()) MENSAJE_OBLIGATORIO else null

    fun validarCorreo(valor: String): String? =
        if (!REGEX_CORREO.matches(valor)) MENSAJE_CORREO else null

    fun validarClave(valor: String): String? =
        if (valor.length < LARGO_MINIMO_CLAVE) MENSAJE_CLAVE else null

    fun validarDireccion(valor: String): String? =
        if (valor.isBlank()) MENSAJE_OBLIGATORIO else null

    fun validarTerminos(aceptados: Boolean): String? =
        if (!aceptados) MENSAJE_TERMINOS else null

    fun validar(estado: UsuarioUiState): UsuarioErrores = UsuarioErrores(
        nombre = validarNombre(estado.nombre),
        correo = validarCorreo(estado.correo),
        clave = validarClave(estado.clave),
        direccion = validarDireccion(estado.direccion),
        terminos = validarTerminos(estado.aceptaTerminos)
    )

    companion object {
        const val LARGO_MINIMO_CLAVE = 6
        const val MENSAJE_OBLIGATORIO = "Campo obligatorio"
        const val MENSAJE_CORREO = "Correo inválido"
        const val MENSAJE_CLAVE = "Debe tener al menos 6 caracteres"
        const val MENSAJE_TERMINOS = "Debes aceptar los términos"

        // mismo patron que android.util.Patterns.EMAIL_ADDRESS
        private val REGEX_CORREO = Regex(
            "[a-zA-Z0-9+._%\\-]{1,256}" +
                "@" +
                "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                "(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+"
        )
    }
}
