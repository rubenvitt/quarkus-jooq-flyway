package dev.rubeen.java.config

import io.github.thibaultmeyer.cuid.CUID

fun generateId(): String {
    return CUID.randomCUID2().toString()
}