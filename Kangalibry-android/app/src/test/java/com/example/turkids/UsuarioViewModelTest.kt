package com.example.turkids

import com.example.turkids.model.CampoUsuario
import com.example.turkids.model.UsuarioErrores
import com.example.turkids.model.ValidadorUsuario
import com.example.turkids.viewmodel.UsuarioViewModel
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class UsuarioViewModelTest : StringSpec({

    fun nuevoViewModel() = UsuarioViewModel(ValidadorUsuario())

    fun UsuarioViewModel.completarFormularioValido() {
        onNombreChange("Ana")
        onCorreoChange("ana@correo.cl")
        onClaveChange("secreta1")
        onDireccionChange("Calle 1")
        onAceptarTerminosChange(true)
    }

    "al abrir el formulario no se muestra ningun error" {
        val estado = nuevoViewModel().estado.value

        estado.camposTocados shouldBe emptySet<CampoUsuario>()
        estado.errores.sinErrores shouldBe true
    }

    "escribir en un campo sin salir de el no muestra su error" {
        val viewModel = nuevoViewModel()

        viewModel.onCorreoChange("a")

        viewModel.estado.value.correo shouldBe "a"
        viewModel.estado.value.errores.correo.shouldBeNull()
    }

    "al salir de un campo invalido aparece su error y no el de los demas" {
        val viewModel = nuevoViewModel()

        viewModel.onCampoVisitado(CampoUsuario.CORREO)

        val errores = viewModel.estado.value.errores
        errores.correo shouldBe ValidadorUsuario.MENSAJE_CORREO
        errores.nombre.shouldBeNull()
        errores.clave.shouldBeNull()
        errores.direccion.shouldBeNull()
        errores.terminos.shouldBeNull()
    }

    "el error del correo se actualiza en tiempo real mientras se escribe" {
        val viewModel = nuevoViewModel()
        viewModel.onCampoVisitado(CampoUsuario.CORREO)

        viewModel.onCorreoChange("ana@correo")
        viewModel.estado.value.errores.correo shouldBe ValidadorUsuario.MENSAJE_CORREO

        viewModel.onCorreoChange("ana@correo.cl")
        viewModel.estado.value.errores.correo.shouldBeNull()
    }

    "la clave exige 6 caracteres y el error desaparece al completarlos" {
        val viewModel = nuevoViewModel()
        viewModel.onCampoVisitado(CampoUsuario.CLAVE)

        viewModel.onClaveChange("12345")
        viewModel.estado.value.errores.clave shouldBe ValidadorUsuario.MENSAJE_CLAVE

        viewModel.onClaveChange("123456")
        viewModel.estado.value.errores.clave.shouldBeNull()
    }

    "nombre y direccion en blanco son invalidos" {
        val viewModel = nuevoViewModel()
        viewModel.onCampoVisitado(CampoUsuario.NOMBRE)
        viewModel.onCampoVisitado(CampoUsuario.DIRECCION)

        viewModel.onNombreChange("   ")
        viewModel.onDireccionChange("")

        viewModel.estado.value.errores.nombre shouldBe ValidadorUsuario.MENSAJE_OBLIGATORIO
        viewModel.estado.value.errores.direccion shouldBe ValidadorUsuario.MENSAJE_OBLIGATORIO

        viewModel.onNombreChange("Ana")
        viewModel.estado.value.errores.nombre.shouldBeNull()
    }

    "marcar o desmarcar los terminos muestra o quita su error de inmediato" {
        val viewModel = nuevoViewModel()

        viewModel.onAceptarTerminosChange(false)
        viewModel.estado.value.errores.terminos shouldBe ValidadorUsuario.MENSAJE_TERMINOS

        viewModel.onAceptarTerminosChange(true)
        viewModel.estado.value.errores.terminos.shouldBeNull()
    }

    "validarFormulario con el formulario vacio falla y muestra todos los errores" {
        val viewModel = nuevoViewModel()

        val valido = viewModel.validarFormulario()

        valido shouldBe false
        val errores = viewModel.estado.value.errores
        errores.nombre shouldBe ValidadorUsuario.MENSAJE_OBLIGATORIO
        errores.correo shouldBe ValidadorUsuario.MENSAJE_CORREO
        errores.clave shouldBe ValidadorUsuario.MENSAJE_CLAVE
        errores.direccion shouldBe ValidadorUsuario.MENSAJE_OBLIGATORIO
        errores.terminos shouldBe ValidadorUsuario.MENSAJE_TERMINOS
    }

    "validarFormulario con datos correctos es valido y no deja errores" {
        val viewModel = nuevoViewModel()
        viewModel.completarFormularioValido()

        viewModel.validarFormulario() shouldBe true
        viewModel.estado.value.errores.sinErrores shouldBe true
    }

    "limpiar devuelve el formulario a su estado inicial" {
        val viewModel = nuevoViewModel()
        viewModel.completarFormularioValido()

        viewModel.limpiar()

        val estado = viewModel.estado.value
        estado.nombre shouldBe ""
        estado.correo shouldBe ""
        estado.aceptaTerminos shouldBe false
        estado.camposTocados shouldBe emptySet<CampoUsuario>()
        estado.errores.sinErrores shouldBe true
    }

    "el ViewModel delega las reglas en el validador" {
        val validador = mockk<ValidadorUsuario>()
        every { validador.validar(any()) } returns UsuarioErrores(nombre = "Error simulado")
        val viewModel = UsuarioViewModel(validador)

        viewModel.onCampoVisitado(CampoUsuario.NOMBRE)

        viewModel.estado.value.errores.nombre shouldBe "Error simulado"
        verify(atLeast = 1) { validador.validar(any()) }
    }
})
