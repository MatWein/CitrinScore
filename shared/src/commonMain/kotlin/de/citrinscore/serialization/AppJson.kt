package de.citrinscore.serialization

import kotlinx.serialization.json.Json

val AppJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}
