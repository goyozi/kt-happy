package io.github.goyozi.kthappy

class Scope<T> {
    val stack = mutableListOf(Layer<T>())

    fun enter() {
        stack.add(Layer())
    }

    fun leave() {
        stack.removeLast()
    }

    fun set(id: String, value: T) {
        stack.last().bindings[id] = value
    }

    fun get(id: String) = stack.findLast { it.bindings.containsKey(id) }?.bindings?.get(id)
        ?: throw IllegalStateException("Unknown identifier: $id")
}

class Layer<T> {
    val bindings = mutableMapOf<String, T>()
}