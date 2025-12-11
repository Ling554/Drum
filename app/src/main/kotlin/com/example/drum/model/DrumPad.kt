package com.example.drum.model

data class DrumPad(
    val id: Int,
    val soundPath: String,
    val image: String,
    val position: Pair<Float, Float>,
    val size: Pair<Float, Float>,
    val name: String
)
