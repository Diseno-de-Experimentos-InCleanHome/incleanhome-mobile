package com.incleanhome.mobile.core.network

import com.google.gson.JsonParser
import retrofit2.HttpException

fun userMessage(exception: HttpException, resource: String = "la operación"): String =
    serverErrorMessage(exception) ?: when (exception.code()) {
        400 -> "Los datos enviados no son válidos."
        401 -> "La sesión no es válida. Vuelve a iniciar sesión."
        403 -> "No tienes permiso para realizar esta acción."
        404 -> "No se encontró el recurso solicitado."
        409 -> "La operación entra en conflicto con el estado actual."
        in 500..599 -> "El servidor no está disponible. Inténtalo nuevamente."
        else -> "No se pudo completar $resource."
    }

internal fun serverErrorMessage(exception: HttpException): String? = runCatching {
    exception.response()?.errorBody()?.string().orEmpty()
}.getOrDefault("").let(::serverErrorMessage)

internal fun serverErrorMessage(body: String): String? = runCatching {
    val json = JsonParser.parseString(body).asJsonObject
    sequenceOf("error", "message", "detail")
        .mapNotNull { key -> json.get(key)?.takeIf { it.isJsonPrimitive }?.asString }
        .map(String::trim)
        .firstOrNull(String::isNotEmpty)
}.getOrNull()
