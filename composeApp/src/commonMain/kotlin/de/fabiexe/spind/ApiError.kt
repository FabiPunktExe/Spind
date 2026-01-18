package de.fabiexe.spind

import androidx.compose.material3.SnackbarHostState
import de.fabiexe.spind.composeapp.generated.resources.Res
import de.fabiexe.spind.composeapp.generated.resources.allStringResources
import de.fabiexe.spind.endpoint.ErrorResponse
import org.jetbrains.compose.resources.getString

suspend fun ApiError.show(snackbarHostState: SnackbarHostState) {
    val resource = Res.allStringResources["ApiError_$code"]
    if (resource != null) {
        snackbarHostState.showSnackbar(getString(resource))
    } else {
        snackbarHostState.showSnackbar("Error: $name")
    }
}

suspend inline fun ErrorResponse.show(snackbarHostState: SnackbarHostState) {
    error.show(snackbarHostState)
}

suspend inline fun Either<*, ErrorResponse>.show(snackbarHostState: SnackbarHostState) {
    if (this is Either.Right) {
        value.show(snackbarHostState)
    }
}