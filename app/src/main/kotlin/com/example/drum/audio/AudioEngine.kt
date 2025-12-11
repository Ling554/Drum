package com.example.drum.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import kotlin.math.min

/**
 * AudioEngine provides zero-delay drum sound playback using AudioTrack.
 * Optimized for low-latency drum sample playback with minimal processing overhead.
 */
class AudioEngine {

    private var audioTrack: AudioTrack? = null
    private val audioSamples = mutableMapOf<String, FloatArray>()
    private var isInitialized = false

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val BUFFER_SIZE_MULTIPLIER = 2
    }

    /**
     * Initialize the AudioEngine with optimal settings for low-latency playback.
     */
    fun init() {
        if (isInitialized) return

        val bufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val adjustedBufferSize = bufferSize * BUFFER_SIZE_MULTIPLIER

        audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_CONFIG)
                .setEncoding(AUDIO_FORMAT)
                .build(),
            adjustedBufferSize,
            AudioTrack.MODE_STREAM,
            android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
        )

        audioTrack?.play()
        isInitialized = true
    }

    /**
     * Load a drum sound sample from file.
     * Supports WAV and MP3 formats with automatic decoding.
     *
     * @param sampleId Unique identifier for the sample
     * @param filePath Path to the audio file
     */
    fun loadSample(sampleId: String, filePath: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                throw IllegalArgumentException("Audio file not found: $filePath")
            }

            val samples = when (file.extension.lowercase()) {
                "wav" -> loadWavSample(file)
                "mp3" -> loadMp3Sample(file)
                else -> throw UnsupportedOperationException("Unsupported audio format: ${file.extension}")
            }

            audioSamples[sampleId] = samples
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Play a loaded drum sound with zero-delay processing.
     *
     * @param sampleId The ID of the sample to play
     * @param gain Volume adjustment (0.0 to 1.0)
     */
    fun playSample(sampleId: String, gain: Float = 1.0f) {
        if (!isInitialized) {
            init()
        }

        val samples = audioSamples[sampleId] ?: run {
            System.err.println("Sample not found: $sampleId")
            return
        }

        val adjustedSamples = if (gain != 1.0f) {
            FloatArray(samples.size) { samples[it] * gain }
        } else {
            samples
        }

        // Convert float samples to PCM 16-bit format
        val pcmData = convertFloatToPcm16(adjustedSamples)

        // Write to AudioTrack with minimal latency
        audioTrack?.write(pcmData, 0, pcmData.size, AudioTrack.WRITE_BLOCKING)
    }

    /**
     * Play multiple samples simultaneously (for layered drum sounds).
     *
     * @param sampleIds List of sample IDs to play
     * @param gains List of gain values corresponding to each sample
     */
    fun playMultipleSamples(sampleIds: List<String>, gains: List<Float> = List(sampleIds.size) { 1.0f }) {
        if (!isInitialized) {
            init()
        }

        if (sampleIds.isEmpty()) return

        // Find the maximum sample length
        val maxLength = sampleIds.mapNotNull { audioSamples[it]?.size ?: 0 }.maxOrNull() ?: 0
        if (maxLength == 0) return

        // Mix all samples
        val mixedSamples = FloatArray(maxLength)
        sampleIds.forEachIndexed { index, sampleId ->
            val samples = audioSamples[sampleId] ?: return@forEachIndexed
            val gain = if (index < gains.size) gains[index] else 1.0f

            for (i in samples.indices) {
                mixedSamples[i] += samples[i] * gain
            }
        }

        // Normalize to prevent clipping
        normalizeAudio(mixedSamples)

        // Convert and play
        val pcmData = convertFloatToPcm16(mixedSamples)
        audioTrack?.write(pcmData, 0, pcmData.size, AudioTrack.WRITE_BLOCKING)
    }

    /**
     * Stop playback and release resources.
     */
    fun release() {
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
        audioSamples.clear()
        isInitialized = false
    }

    /**
     * Clear all loaded samples from memory.
     */
    fun clearSamples() {
        audioSamples.clear()
    }

    /**
     * Remove a specific sample from memory.
     *
     * @param sampleId The ID of the sample to remove
     */
    fun removeSample(sampleId: String) {
        audioSamples.remove(sampleId)
    }

    /**
     * Check if a sample is loaded.
     *
     * @param sampleId The ID to check
     * @return True if the sample is loaded
     */
    fun isSampleLoaded(sampleId: String): Boolean {
        return audioSamples.containsKey(sampleId)
    }

    /**
     * Get the current playback state.
     *
     * @return True if AudioTrack is playing
     */
    fun isPlaying(): Boolean {
        return audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING
    }

    // Private helper methods

    private fun loadWavSample(file: File): FloatArray {
        val wavReader = WavReader(file)
        return wavReader.readSamples()
    }

    private fun loadMp3Sample(file: File): FloatArray {
        val extractor = MediaExtractor()
        extractor.setDataSource(file.absolutePath)

        var audioTrackIndex = -1
        var format: MediaFormat? = null

        for (i in 0 until extractor.trackCount) {
            val trackFormat = extractor.getTrackFormat(i)
            if (trackFormat.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) {
                audioTrackIndex = i
                format = trackFormat
                break
            }
        }

        if (audioTrackIndex == -1 || format == null) {
            throw RuntimeException("No audio track found in file")
        }

        extractor.selectTrack(audioTrackIndex)

        val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME) ?: "audio/mpeg")
        decoder.configure(format, null, null, 0)
        decoder.start()

        val samples = mutableListOf<Float>()
        val inputBuffers = decoder.inputBuffers
        val outputBuffers = decoder.outputBuffers
        val bufferInfo = MediaCodec.BufferInfo()

        var isDecoderDone = false
        var isExtractorDone = false

        while (!isDecoderDone || !isExtractorDone) {
            if (!isExtractorDone) {
                val inputIndex = decoder.dequeueInputBuffer(10000)
                if (inputIndex >= 0) {
                    val inputBuffer = inputBuffers[inputIndex]
                    val sampleSize = extractor.readSampleData(inputBuffer, 0)

                    if (sampleSize < 0) {
                        decoder.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isExtractorDone = true
                    } else {
                        decoder.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            val outputIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000)
            if (outputIndex >= 0) {
                val outputBuffer = outputBuffers[outputIndex]
                outputBuffer.position(bufferInfo.offset)
                outputBuffer.limit(bufferInfo.offset + bufferInfo.size)

                val pcmData = ShortArray(bufferInfo.size / 2)
                outputBuffer.asShortBuffer().get(pcmData)

                for (sample in pcmData) {
                    samples.add(sample / 32768.0f)
                }

                decoder.releaseOutputBuffer(outputIndex, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    isDecoderDone = true
                }
            }
        }

        decoder.stop()
        decoder.release()
        extractor.release()

        return samples.toFloatArray()
    }

    private fun convertFloatToPcm16(samples: FloatArray): ShortArray {
        val pcm = ShortArray(samples.size)
        for (i in samples.indices) {
            val clipped = samples[i].coerceIn(-1.0f, 1.0f)
            pcm[i] = (clipped * 32767).toInt().toShort()
        }
        return pcm
    }

    private fun normalizeAudio(samples: FloatArray) {
        val maxAbsValue = samples.maxByOrNull { kotlin.math.abs(it) } ?: return
        if (maxAbsValue > 1.0f) {
            val factor = 1.0f / maxAbsValue
            for (i in samples.indices) {
                samples[i] *= factor
            }
        }
    }
}

/**
 * Simple WAV file reader for loading uncompressed audio.
 */
private class WavReader(private val file: File) {

    fun readSamples(): FloatArray {
        val data = file.readBytes()
        var offset = 0

        // Read RIFF header
        if (String(data.sliceArray(offset until offset + 4)) != "RIFF") {
            throw RuntimeException("Invalid WAV file: missing RIFF header")
        }
        offset += 8

        // Read WAVE header
        if (String(data.sliceArray(offset until offset + 4)) != "WAVE") {
            throw RuntimeException("Invalid WAV file: missing WAVE header")
        }
        offset += 4

        var numChannels = 1
        var sampleRate = 44100
        var bitsPerSample = 16

        // Find and read fmt chunk
        while (offset < data.size) {
            val chunkId = String(data.sliceArray(offset until offset + 4))
            offset += 4
            val chunkSize = bytesToInt(data, offset)
            offset += 4

            if (chunkId == "fmt ") {
                numChannels = bytesToShort(data, offset).toInt()
                sampleRate = bytesToInt(data, offset + 4)
                bitsPerSample = bytesToShort(data, offset + 14).toInt()
                offset += chunkSize
                break
            } else {
                offset += chunkSize
            }
        }

        // Find data chunk
        while (offset < data.size) {
            val chunkId = String(data.sliceArray(offset until offset + 4))
            offset += 4
            val chunkSize = bytesToInt(data, offset)
            offset += 4

            if (chunkId == "data") {
                val numSamples = chunkSize / (bitsPerSample / 8) / numChannels
                val samples = FloatArray(numSamples)

                when (bitsPerSample) {
                    16 -> {
                        for (i in 0 until numSamples) {
                            val sample = bytesToShort(data, offset + i * 2).toInt()
                            samples[i] = sample / 32768.0f
                        }
                    }
                    8 -> {
                        for (i in 0 until numSamples) {
                            val sample = (data[offset + i].toInt() and 0xFF) - 128
                            samples[i] = sample / 128.0f
                        }
                    }
                }

                return samples
            } else {
                offset += chunkSize
            }
        }

        throw RuntimeException("Invalid WAV file: no data chunk found")
    }

    private fun bytesToShort(data: ByteArray, offset: Int): Short {
        return ((data[offset + 1].toInt() and 0xFF) shl 8 or (data[offset].toInt() and 0xFF)).toShort()
    }

    private fun bytesToInt(data: ByteArray, offset: Int): Int {
        return ((data[offset + 3].toInt() and 0xFF) shl 24 or
                ((data[offset + 2].toInt() and 0xFF) shl 16) or
                ((data[offset + 1].toInt() and 0xFF) shl 8) or
                (data[offset].toInt() and 0xFF))
    }
}
