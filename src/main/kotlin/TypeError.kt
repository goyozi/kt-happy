package io.github.goyozi.kthappy

data class TypeError(val location: String, val expectedType: String, val actualType: String)