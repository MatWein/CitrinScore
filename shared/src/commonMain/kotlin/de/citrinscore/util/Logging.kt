package de.citrinscore.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.slf4j.Logger

fun <T> CoroutineScope.logChanges(values: Flow<T>, logger: Logger, message: String) =
    launch { values.distinctUntilChanged().collect { logger.info(message, it) } }
