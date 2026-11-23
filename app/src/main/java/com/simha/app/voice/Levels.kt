package com.simha.app.voice

object Levels {
    fun fromDb(db: Float): Float = ((db + 2f) / 12f).coerceIn(0f, 1f)

    fun fromRms(rms: Double): Float = Math.sqrt((rms / 6000.0).coerceIn(0.0, 1.0)).toFloat()

    fun envelope(wav: ByteArray, windowMs: Int = 40): FloatArray {
        if (wav.size < 44 || String(wav, 0, 4, Charsets.US_ASCII) != "RIFF" || String(wav, 8, 4, Charsets.US_ASCII) != "WAVE") return FloatArray(0)
        fun i16(o: Int) = (wav[o].toInt() and 0xff) or (wav[o + 1].toInt() shl 8)
        fun i32(o: Int) = i16(o) or (i16(o + 2) shl 16)
        var pos = 12
        var rate = 0
        var ch = 1
        var bits = 16
        while (pos + 8 <= wav.size) {
            val id = String(wav, pos, 4, Charsets.US_ASCII)
            val len = i32(pos + 4)
            if (id == "fmt " && pos + 24 <= wav.size) {
                ch = i16(pos + 10).coerceAtLeast(1)
                rate = i32(pos + 12)
                bits = i16(pos + 22)
            } else if (id == "data") {
                if (bits != 16 || rate <= 0) return FloatArray(0)
                val start = pos + 8
                val end = if (len <= 0 || start + len > wav.size) wav.size else start + len
                val win = (rate * windowMs / 1000) * ch * 2
                if (win <= 0) return FloatArray(0)
                val n = (end - start) / win
                return FloatArray(n) { w ->
                    var s = 0.0
                    var c = 0
                    var o = start + w * win
                    val e = o + win
                    while (o + 1 < e) {
                        val v = (wav[o].toInt() and 0xff or (wav[o + 1].toInt() shl 8)).toShort().toDouble()
                        s += v * v
                        c++
                        o += 2
                    }
                    if (c == 0) 0f else fromRms(Math.sqrt(s / c))
                }
            }
            if (len < 0) return FloatArray(0)
            pos += 8 + len + (len and 1)
        }
        return FloatArray(0)
    }
}
