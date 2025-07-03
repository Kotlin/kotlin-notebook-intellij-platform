package org.jetbrains.kotlinx.jupyter.intellij.utils

import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

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

@Suppress("unused")
fun <T> getField(
    className: String,
    fieldName: String,
    instance: Any?,
): T {
    val clazz = Class.forName(className)
    val field = clazz.getDeclaredField(fieldName)

    field.isAccessible = true

    @Suppress("UNCHECKED_CAST")
    return field.get(instance) as T
}

/**
 * If this object has a Kotlin property with this name, returns its value.
 * Otherwise, returns null.
 */
@Suppress("unused")
fun Any.getPropertyValue(name: String): Any? {
    val kClass = this::class
    val property = kClass.memberProperties.find { it.name == name } ?: return null
    property.isAccessible = true
    return property.getter.call(this)
}

fun argumentsOf(vararg args: Pair<Class<*>, Any?>): List<Argument> {
    return args.map { Argument(it.first, it.second) }
}

class Argument(
    val type: Class<*>,
    val value: Any?,
)
