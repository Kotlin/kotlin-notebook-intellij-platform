package org.jetbrains.kotlinx.jupyter.intellij.util

fun <T> invokeMethod(
    className: String,
    methodName: String,
    instance: Any?,
    arguments: List<Argument>,
): T {
    val clazz = Class.forName(className)
    val method = clazz.getDeclaredMethod(methodName, *arguments.map { it.type }.toTypedArray())

    method.isAccessible = true

    @Suppress("UNCHECKED_CAST")
    return method.invoke(instance, *arguments.map { it.value }.toTypedArray()) as T
}

fun argumentsOf(vararg args: Pair<Class<*>, Any?>): List<Argument> {
    return args.map { Argument(it.first, it.second) }
}

class Argument(
    val type: Class<*>,
    val value: Any?,
)
