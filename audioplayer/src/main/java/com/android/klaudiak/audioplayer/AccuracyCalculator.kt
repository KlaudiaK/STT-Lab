package com.android.klaudiak.audioplayer

import java.io.File

object AccuracyCalculator {

    fun calculateWERBasedOnReferenceFile(refFile: File, hypFile: File) {
        /*val refFile = "ref.txt"  // path to reference file
        val hypFile = "hyp.txt"  // path to hypothesis file

        val refMap = loadTranscriptions(refFile)
        val hypMap = loadTranscriptions(hypFile)*/

        val refMap = loadTranscriptions(refFile)
        val hypMap = loadTranscriptions(hypFile)

        var totalWords = 0
        var totalErrors = 0

        for ((filename, refText) in refMap) {
            val hypText = hypMap[filename]
            if (hypText != null) {
                val (errors, words) = computeWER(refText, hypText)
                totalErrors += errors
                totalWords += words
                println("$filename WER: ${"%.2f".format(errors.toDouble() / words * 100)}%")
            } else {
                println("$filename missing in hypothesis file.")
            }
        }

        println("Overall WER: ${"%.2f".format(totalErrors.toDouble() / totalWords * 100)}%")
    }

    fun loadTranscriptions(file: File): Map<String, String> {
        return file.readLines()
            .mapNotNull { line ->
                val parts = line.trim().split("\\s+".toRegex(), limit = 2)
                if (parts.size == 2) parts[0] to parts[1].uppercase() else null
            }
            .toMap()
    }

    private fun computeWER(ref: String, hyp: String): Pair<Int, Int> {
        val r = ref.trim().split("\\s+".toRegex())
        val h = hyp.trim().split("\\s+".toRegex())

        val d = Array(r.size + 1) { IntArray(h.size + 1) }

        for (i in 0..r.size) d[i][0] = i
        for (j in 0..h.size) d[0][j] = j

        for (i in 1..r.size) {
            for (j in 1..h.size) {
                val cost = if (r[i - 1] == h[j - 1]) 0 else 1
                d[i][j] = minOf(
                    d[i - 1][j] + 1,       // deletion
                    d[i][j - 1] + 1,       // insertion
                    d[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return Pair(d[r.size][h.size], r.size) // total errors, total words
    }
}