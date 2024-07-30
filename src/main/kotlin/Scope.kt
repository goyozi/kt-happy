package io.github.goyozi.kthappy

class Scope<T> {
    val stack = ArrayDeque<Layer<T>>(listOf(Layer()))

    fun enter() {
        stack.add(Layer(stack.last()))
    }

    fun enter(parent: Layer<T>) {
        stack.add(Layer(parent))
    }

    fun leave() {
        stack.removeLast()
    }

    fun define(id: String, value: T) {
        stack.last().define(id, value)
    }

    fun assign(id: String, value: T) {
        stack.last().assign(id, value)
    }

    fun get(id: String) = stack.last().get(id)
}

data class Layer<T>(val parent: Layer<T>? = null) {

    val bindings: MutableMap<String, T> = HashMap()

    fun define(id: String, value: T) {
        // todo: can't do it because my exec implementation is stupid
//        if (bindings.containsKey(id)) throw IllegalStateException("Already defined: $id")
        bindings[id] = value
    }

    fun assign(id: String, value: T) {
        if (bindings.containsKey(id)) bindings[id] = value
        else if (parent != null) parent.assign(id, value)
        else throw IllegalStateException("Unknown identifier: $id")
    }

    fun get(id: String): T = bindings[id]
        ?: parent?.get(id)
        ?: throw IllegalStateException("Unknown identifier: $id")
}