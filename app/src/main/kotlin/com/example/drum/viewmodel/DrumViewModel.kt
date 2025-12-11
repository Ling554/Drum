package com.example.drum.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.drum.audio.AudioEngine
import com.example.drum.model.DrumPad
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * DrumViewModel manages the state and logic for the drum application.
 * Handles drum pad management, audio playback, and customization features.
 */
class DrumViewModel : ViewModel() {

    private val audioEngine = AudioEngine()

    // UI State
    private val _drumPads = MutableStateFlow<List<DrumPad>>(emptyList())
    val drumPads: StateFlow<List<DrumPad>> = _drumPads.asStateFlow()

    private val _selectedPadId = MutableStateFlow<Int?>(null)
    val selectedPadId: StateFlow<Int?> = _selectedPadId.asStateFlow()

    private val _backgroundColor = MutableStateFlow<String>("")
    val backgroundColor: StateFlow<String> = _backgroundColor.asStateFlow()

    private val _menuOpen = MutableStateFlow(false)
    val menuOpen: StateFlow<Boolean> = _menuOpen.asStateFlow()

    private val _currentMusicPath = MutableStateFlow<String?>(null)
    val currentMusicPath: StateFlow<String?> = _currentMusicPath.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    init {
        audioEngine.init()
        initializeDefaultPads()
    }

    /**
     * Initialize default drum pads in standard drum kit layout
     */
    private fun initializeDefaultPads() {
        val defaultPads = listOf(
            // First row
            DrumPad(1, "", "", Pair(0.1f, 0.15f), Pair(0.2f, 0.2f), "Kick"),
            DrumPad(2, "", "", Pair(0.4f, 0.15f), Pair(0.2f, 0.2f), "Snare"),
            DrumPad(3, "", "", Pair(0.7f, 0.15f), Pair(0.2f, 0.2f), "Hi-Hat"),
            // Second row
            DrumPad(4, "", "", Pair(0.1f, 0.5f), Pair(0.2f, 0.2f), "Tom High"),
            DrumPad(5, "", "", Pair(0.4f, 0.5f), Pair(0.2f, 0.2f), "Tom Mid"),
            DrumPad(6, "", "", Pair(0.7f, 0.5f), Pair(0.2f, 0.2f), "Tom Low"),
            // Third row
            DrumPad(7, "", "", Pair(0.1f, 0.8f), Pair(0.2f, 0.2f), "Cymbal"),
            DrumPad(8, "", "", Pair(0.4f, 0.8f), Pair(0.2f, 0.2f), "Perc 1"),
            DrumPad(9, "", "", Pair(0.7f, 0.8f), Pair(0.2f, 0.2f), "Perc 2"),
        )
        _drumPads.value = defaultPads
    }

    /**
     * Play a drum sound by pad ID
     */
    fun playPad(padId: Int) {
        viewModelScope.launch {
            val pad = _drumPads.value.find { it.id == padId } ?: return@launch
            if (pad.soundPath.isNotEmpty()) {
                audioEngine.playSample("pad_$padId")
            }
        }
    }

    /**
     * Load a custom sound for a specific pad
     */
    fun loadSoundForPad(padId: Int, soundPath: String) {
        viewModelScope.launch {
            audioEngine.loadSample("pad_$padId", soundPath)
            _drumPads.value = _drumPads.value.map { pad ->
                if (pad.id == padId) pad.copy(soundPath = soundPath) else pad
            }
        }
    }

    /**
     * Update pad image
     */
    fun updatePadImage(padId: Int, imagePath: String) {
        _drumPads.value = _drumPads.value.map { pad ->
            if (pad.id == padId) pad.copy(image = imagePath) else pad
        }
    }

    /**
     * Update pad position
     */
    fun updatePadPosition(padId: Int, x: Float, y: Float) {
        _drumPads.value = _drumPads.value.map { pad ->
            if (pad.id == padId) pad.copy(position = Pair(x, y)) else pad
        }
    }

    /**
     * Update pad size
     */
    fun updatePadSize(padId: Int, width: Float, height: Float) {
        _drumPads.value = _drumPads.value.map { pad ->
            if (pad.id == padId) pad.copy(size = Pair(width, height)) else pad
        }
    }

    /**
     * Select a pad for editing
     */
    fun selectPad(padId: Int?) {
        _selectedPadId.value = padId
    }

    /**
     * Set background color/image
     */
    fun setBackgroundColor(colorHex: String) {
        _backgroundColor.value = colorHex
    }

    /**
     * Toggle hamburger menu
     */
    fun toggleMenu() {
        _menuOpen.value = !_menuOpen.value
    }

    /**
     * Close menu
     */
    fun closeMenu() {
        _menuOpen.value = false
    }

    /**
     * Load music file from storage
     */
    fun loadMusicFile(filePath: String) {
        _currentMusicPath.value = filePath
    }

    /**
     * Play loaded music
     */
    fun playMusic() {
        _isPlaying.value = true
        // Implementation for music player will be added
    }

    /**
     * Pause music playback
     */
    fun pauseMusic() {
        _isPlaying.value = false
    }

    /**
     * Stop music playback
     */
    fun stopMusic() {
        _isPlaying.value = false
        _currentMusicPath.value = null
    }

    /**
     * Add a new drum pad
     */
    fun addPad(pad: DrumPad) {
        _drumPads.value = _drumPads.value + pad
    }

    /**
     * Remove a drum pad
     */
    fun removePad(padId: Int) {
        _drumPads.value = _drumPads.value.filter { it.id != padId }
        audioEngine.removeSample("pad_$padId")
    }

    /**
     * Reset all pads to default layout
     */
    fun resetLayout() {
        _drumPads.value = emptyList()
        initializeDefaultPads()
    }

    /**
     * Reset all sounds to default (empty)
     */
    fun resetSounds() {
        audioEngine.clearSamples()
        _drumPads.value = _drumPads.value.map { it.copy(soundPath = "") }
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
    }
}
