package com.jcdongalen.peryaodds.shared.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class Observation(
    val id: String,
    val timestamp: Long,
    val hits: List<String>
)
