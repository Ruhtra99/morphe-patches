/**
 * Copyright 2026 Hoo-dles
 * https://github.com/hoo-dles/morphe-patches
 */

package hoodles.morphe.patches.soundcloud.playback.speed

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import hoodles.morphe.patches.soundcloud.shared.Constants

/** Raw IEEE-754 bits of a float, formatted for a smali `const` instruction. */
private fun Float.toSmaliHex() = "0x" + Integer.toHexString(java.lang.Float.floatToIntBits(this))

@Suppress("unused")
val playbackSpeedPatch = bytecodePatch(
    name = "Playback speed",
    description = "Plays all tracks at a custom speed.",
    default = false
) {
    compatibleWith(Constants.COMPATIBILITY)

    val speed by stringOption(
        key = "speed",
        default = "1.25",
        values = mapOf(
            "0.5x" to "0.5",
            "0.75x" to "0.75",
            "1.25x" to "1.25",
            "1.5x" to "1.5",
            "1.75x" to "1.75",
            "2.0x" to "2.0",
            "2.5x" to "2.5",
            "3.0x" to "3.0"
        ),
        title = "Speed",
        description = "The speed at which tracks are played.",
        required = true
    )

    val preservePitch by booleanOption(
        key = "preservePitch",
        default = true,
        title = "Preserve pitch",
        description = "Keeps the pitch of the audio unchanged when the speed is changed. " +
                "Disable for a vinyl-style effect where the pitch rises and falls with the speed.",
        required = true
    )

    execute {
        val speedValue = speed!!.toFloat()
        val pitchValue = if (preservePitch!!) 1.0f else speedValue

        val playerClass = ExoPlayerPrepareFingerprint.classDef

        // Look up setPlaybackParameters() on the same class so we pick up the correct
        // PlaybackParameters type (exoplayer2 vs media3) without hardcoding either.
        val setPlaybackParameters = playerClass.methods.firstOrNull {
            it.name == "setPlaybackParameters" && it.parameterTypes.size == 1
        } ?: throw PatchException("Could not find setPlaybackParameters() in ${playerClass.type}")

        val paramsType = setPlaybackParameters.parameterTypes.first().toString()
        val setParamsRef = "${playerClass.type}->${setPlaybackParameters.name}($paramsType)${setPlaybackParameters.returnType}"

        ExoPlayerPrepareFingerprint.method.apply {
            // Instance method with no parameters: p0 is the only parameter register.
            val locals = implementation!!.registerCount - 1
            if (locals < 3) {
                throw PatchException("prepare() only has $locals free registers, need 3")
            }

            // Inserted at the very start, where the local registers are still unused.
            // setPlaybackParameters() returns early if the value is unchanged, so calling
            // this on every prepare() is cheap.
            addInstructions(0, """
                new-instance v0, $paramsType
                const v1, ${speedValue.toSmaliHex()}
                const v2, ${pitchValue.toSmaliHex()}
                invoke-direct { v0, v1, v2 }, $paramsType-><init>(FF)V
                invoke-virtual { p0, v0 }, $setParamsRef
            """.trimIndent())
        }
    }
}
