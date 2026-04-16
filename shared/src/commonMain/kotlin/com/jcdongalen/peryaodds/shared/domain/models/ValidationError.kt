package com.jcdongalen.peryaodds.shared.domain.models

data class ValidationError(
    val code: ValidationErrorCode,
    val message: String
)

enum class ValidationErrorCode {
    TOO_FEW_HITS, TOO_MANY_HITS, INVALID_OUTCOME, STORAGE_ERROR
}
