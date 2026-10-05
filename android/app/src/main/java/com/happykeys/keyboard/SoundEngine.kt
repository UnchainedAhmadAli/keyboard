package com.happykeys.keyboard

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Synthesizes soft piano-like notes once, then plays them with a SoundPool for low latency. */
class SoundEngine(private val context: Context) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(8)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()
    private val ids = HashMap<Double, Int>()
    private val ready = HashSet<Int>()

    init {
        pool.setOnLoadCompleteListener { _, id, status -> if (status == 0) ready.add(id) }
        val dir = File(context.cacheDir, "notes_v1").apply { mkdirs() }
        for (f in Songs.allFrequencies) {
            val file = File(dir, "n_${(f * 100).toInt()}.wav")
            if (!file.exists()) writeWav(file, f)
            ids[f] = pool.load(file.absolutePath, 1)
        }
    }

    fun play(freq: Double) {
        val id = ids[freq] ?: return
        if (id in ready) pool.play(id, 0.9f, 0.9f, 1, 0, 1f)
    }

    fun release() = pool.release()

    private fun writeWav(file: File, freq: Double) {
        val rate = 22050
        val n = (rate * 1.1).toInt()
        val pcm = ByteBuffer.allocate(n * 2).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0 until n) {
            val t = i.toDouble() / rate
            val attack = if (t < 0.012) t / 0.012 else 1.0
            val env = attack * exp(-3.4 * t)
            val w = tri(freq, t) + 0.35 * sin(2 * PI * freq * 2 * t) + 0.12 * sin(2 * PI * freq * 3 * t)
            val v = (w * env * 0.45 * 32767).toInt().coerceIn(-32768, 32767)
            pcm.putShort(v.toShort())
        }
        val data = pcm.array()
        FileOutputStream(file).use { o ->
            val h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
            h.put("RIFF".toByteArray()); h.putInt(36 + data.size); h.put("WAVE".toByteArray())
            h.put("fmt ".toByteArray()); h.putInt(16); h.putShort(1); h.putShort(1)
            h.putInt(rate); h.putInt(rate * 2); h.putShort(2); h.putShort(16)
            h.put("data".toByteArray()); h.putInt(data.size)
            o.write(h.array()); o.write(data)
        }
    }

    private fun tri(f: Double, t: Double): Double {
        val p = (f * t) % 1.0
        return 4 * kotlin.math.abs(p - 0.5) - 1
    }
}
