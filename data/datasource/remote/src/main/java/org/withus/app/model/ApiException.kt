package org.withus.app.model

class ApiException(val httpCode: Int, override val message: String) : Exception(message)
