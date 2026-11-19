package de.citrinscore.omr

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.opencv.core.Mat
import java.nio.FloatBuffer
import java.nio.LongBuffer

internal const val CANVAS_HEIGHT = 256
internal const val CANVAS_WIDTH = 1280
private const val MAX_SEQUENCE_LENGTH = 608
private const val DECODER_DEPTH = 8
private const val DECODER_HEADS = 8
private const val HEAD_DIMENSION = 64
private const val CACHE_TENSORS = DECODER_DEPTH * 4
private const val MEAN = 0.7931f
private const val STANDARD_DEVIATION = 0.1738f
private const val START_TOKEN = 1L
private const val NO_NOTE_TOKEN = 0L

private val BRANCH_INPUTS = listOf("rhythms", "pitchs", "lifts", "articulations", "slurs")
private val BRANCH_OUTPUTS = listOf("out_rhythms", "out_pitchs", "out_lifts", "out_articulations", "out_slurs", "out_positions")
private const val ATTENTION_OUTPUT = "attention"

internal class Transformer(private val environment: OrtEnvironment, encoderModel: String, decoderModel: String) : AutoCloseable {
    private val encoder = environment.createSession(encoderModel, OrtSession.SessionOptions())
    private val decoder = environment.createSession(decoderModel, OrtSession.SessionOptions())
    private val requestedOutputs = (BRANCH_OUTPUTS + ATTENTION_OUTPUT + List(CACHE_TENSORS) { "cache_out$it" }).toSet()

    fun predict(canvas: Mat): List<EncodedSymbol> {
        val pixels = canvas.bytes()
        val input = FloatBuffer.wrap(FloatArray(pixels.size) { ((pixels[it].toInt() and 0xFF) / 255f - MEAN) / STANDARD_DEVIATION })
        val context = OnnxTensor.createTensor(environment, input, longArrayOf(1, 1, CANVAS_HEIGHT.toLong(), CANVAS_WIDTH.toLong())).use { tensor ->
            encoder.run(mapOf("input" to tensor)).use { result ->
                val output = result[0] as OnnxTensor
                val shape = output.info.shape
                val values = output.floatBuffer
                FloatArray(values.remaining()).also { values.get(it) } to shape
            }
        }
        return decode(context.first, context.second)
    }

    private fun decode(context: FloatArray, shape: LongArray): List<EncodedSymbol> {
        val dimension = shape.last().toInt()
        val fullContext = OnnxTensor.createTensor(environment, FloatBuffer.wrap(context), shape)
        val reducedContext = OnnxTensor.createTensor(environment, FloatBuffer.wrap(context, 0, dimension).slice(), longArrayOf(1, 1, dimension.toLong()))
        var cache: List<OnnxTensor> = List(CACHE_TENSORS) {
            OnnxTensor.createTensor(environment, FloatBuffer.allocate(0), longArrayOf(1, DECODER_HEADS.toLong(), 0, HEAD_DIMENSION.toLong()))
        }
        var previous: OrtSession.Result? = null
        var tokens = listOf(START_TOKEN, NO_NOTE_TOKEN, NO_NOTE_TOKEN, NO_NOTE_TOKEN, NO_NOTE_TOKEN)
        val symbols = mutableListOf<EncodedSymbol>()
        try {
            for (step in 0 until MAX_SEQUENCE_LENGTH) {
                val branchTensors = tokens.map { OnnxTensor.createTensor(environment, LongBuffer.wrap(longArrayOf(it)), longArrayOf(1, 1)) }
                val cacheLength = OnnxTensor.createTensor(environment, LongBuffer.wrap(longArrayOf(step.toLong())), longArrayOf(1))
                val inputs = buildMap {
                    BRANCH_INPUTS.zip(branchTensors).forEach { (name, tensor) -> put(name, tensor) }
                    put("context", if (step == 0) fullContext else reducedContext)
                    put("cache_len", cacheLength)
                    cache.forEachIndexed { index, tensor -> put("cache_in$index", tensor) }
                }
                val result = decoder.run(inputs, requestedOutputs)
                branchTensors.forEach(OnnxTensor::close)
                cacheLength.close()
                if (previous == null) cache.forEach(OnnxTensor::close) else previous.close()
                previous = result
                cache = List(CACHE_TENSORS) { result.get("cache_out$it").get() as OnnxTensor }
                val samples = BRANCH_OUTPUTS.map { argmaxOfLastStep(result.get(it).get() as OnnxTensor) }
                if (samples[0] == Vocabulary.EOS_TOKEN) break
                symbols += EncodedSymbol(
                    rhythm = Vocabulary.rhythm[samples[0]],
                    pitch = Vocabulary.pitch[samples[1]],
                    lift = Vocabulary.lift[samples[2]],
                    articulation = Vocabulary.articulation[samples[3]],
                    slur = Vocabulary.slur[samples[4]],
                    position = Vocabulary.position[samples[5]],
                    location = location(result.get(ATTENTION_OUTPUT).get() as OnnxTensor),
                )
                tokens = samples.take(BRANCH_INPUTS.size).map { it.toLong() }
            }
        } finally {
            previous?.close() ?: cache.forEach(OnnxTensor::close)
            fullContext.close()
            reducedContext.close()
        }
        return symbols
    }

    private fun location(attention: OnnxTensor): SymbolLocation? {
        val values = attention.floatBuffer
        if (values.remaining() < 2) return null
        val x = values[0].toDouble()
        val y = values[1].toDouble()
        return if (x.isNaN() || y.isNaN()) null else SymbolLocation(x, y)
    }

    private fun argmaxOfLastStep(tensor: OnnxTensor): Int {
        val size = tensor.info.shape.last().toInt()
        val values = tensor.floatBuffer
        val offset = values.remaining() - size
        var best = 0
        var bestValue = values[offset]
        for (i in 1 until size) {
            if (values[offset + i] > bestValue) {
                best = i
                bestValue = values[offset + i]
            }
        }
        return best
    }

    override fun close() {
        encoder.close()
        decoder.close()
    }
}
