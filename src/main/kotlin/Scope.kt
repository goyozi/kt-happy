package io.github.goyozi.kthappy

class Scope {
    val stack = mutableListOf(Layer())

    fun enter() {
        stack.add(Layer())
    }

    fun leave() {
        stack.removeLast()
    }

    fun set(id: String, value: String) {
        stack.last().bindings[id] = value
    }

    fun get(id: String) = stack.findLast { it.bindings.containsKey(id) }?.bindings?.get(id)
        ?: throw IllegalStateException("Unknown identifier: $id")
}

class Layer {
    val bindings = mutableMapOf<String, String>()
}