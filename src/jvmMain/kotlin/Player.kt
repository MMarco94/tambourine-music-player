import javazoom.jl.decoder.*
import javazoom.jl.player.AudioDevice
import javazoom.jl.player.FactoryRegistry
import java.io.InputStream
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds


/**
 * See javazoom.jl.player.Player, but:
 * - Single thread
 * - With "seek" methods (see https://github.com/radinshayanfar/MyJlayer/blob/77e913ad9cacd37f43ed5616ed88c001353e4cd0/src/javazoom/jl/player/Player.java)
 */
class Player(
    stream: InputStream?,
    val device: AudioDevice = FactoryRegistry.systemRegistry().createAudioDevice()
) {
    private val bitstream: Bitstream
    private val decoder: Decoder
    private var _decodedPositionMs: Float = 0f
    private var _deviceOffsetMs: Float = 0f
    private var _lastFrameMs: Float = 0f

    val frameLength get() = ceil(_lastFrameMs).roundToInt().milliseconds
    val decodedPosition get() = _decodedPositionMs.roundToInt().milliseconds
    val devicePosition get() = _deviceOffsetMs.roundToInt().milliseconds + device.position.milliseconds
    val deviceLag get() = (_decodedPositionMs - _deviceOffsetMs - device.position).roundToInt().milliseconds

    init {
        bitstream = Bitstream(stream)
        decoder = Decoder()
        device.open(decoder)
    }


    fun play() {
        play(Int.MAX_VALUE)
    }

    fun seek(position: Duration) {
        while (decodedPosition < position) {
            skipFrame() ?: return
        }
    }

    fun play(frames: Int): Boolean {
        var frames = frames
        var ret = true
        while (frames-- > 0 && ret) {
            ret = playFrame()
        }
        if (!ret) {
            device.flush()
            close()
        }
        return ret
    }

    fun close() {
        device.close()
        bitstream.close()
    }

    private fun readFrame(): Header? {
        return bitstream.readFrame()?.also {
            _lastFrameMs = it.ms_per_frame()
            this._decodedPositionMs += _lastFrameMs
        }
    }

    fun skipFrame(): Header? {
        return readFrame()?.also {
            this._deviceOffsetMs += _lastFrameMs
            bitstream.closeFrame()
        }
    }

    fun playFrame(): Boolean {
        val h = readFrame() ?: return false
        val output = decoder.decodeFrame(h, bitstream) as SampleBuffer
        device.write(output.buffer, 0, output.bufferLength)
        bitstream.closeFrame()
        return true
    }
}
