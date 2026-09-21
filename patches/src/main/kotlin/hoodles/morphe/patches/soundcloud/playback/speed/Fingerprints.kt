/**
 * Copyright 2026 Hoo-dles
 * https://github.com/hoo-dles/morphe-patches
 */

package hoodles.morphe.patches.soundcloud.playback.speed

import app.morphe.patcher.Fingerprint

/**
 * ExoPlayerImpl.prepare().
 *
 * Matched by suffix so it works for both `com/google/android/exoplayer2/ExoPlayerImpl`
 * and `androidx/media3/exoplayer/ExoPlayerImpl`.
 *
 * prepare() is a public player API, so it is guaranteed to run on the player's application
 * thread. That makes it a safe place to call setPlaybackParameters(), unlike the constructor
 * (which may run on a thread that isn't the player's application looper).
 */
object ExoPlayerPrepareFingerprint : Fingerprint(
    definingClass = "/ExoPlayerImpl;",
    name = "prepare",
    parameters = listOf(),
    returnType = "V"
)
