package com.incleanhome.mobile.core.network

import retrofit2.HttpException

/** User-facing, non-sensitive HTTP errors. Backend business validation is still represented by 400. */
fun userMessage(exception: HttpException, resource: String = "la operación"): String = when (exception.code()) {
    400 -> "Los datos enviados no son válidos."
    401 -> "La sesión no es válida. Vuelve a iniciar sesión."
    403 -> "No tienes permiso para realizar esta acción."
    404 -> "No se encontró el recurso solicitado."
    409 -> "La operación entra en conflicto con el estado actual."
    in 500..599 -> "El servidor no está disponible. Inténtalo nuevamente."
    else -> "No se pudo completar $resource."
}
