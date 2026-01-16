package de.fabiexe.spind

class ApiException(val error: ApiError) : Exception()