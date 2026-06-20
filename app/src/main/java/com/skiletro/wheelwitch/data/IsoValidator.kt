package com.skiletro.wheelwitch.data

import android.content.Context
import android.net.Uri
import com.skiletro.wheelwitch.data.GameTypeParser.GameFormat
import com.skiletro.wheelwitch.data.GameTypeParser.GameInfo
import java.io.File

/** Outcome of validating a user-picked Mario Kart Wii ROM URI. */
sealed interface IsoValidationResult {
  data class Valid(val gameInfo: GameInfo) : IsoValidationResult
  data object Unreachable : IsoValidationResult
  data object NotFound : IsoValidationResult
  data object Invalid : IsoValidationResult
}

/**
 * Validates a user-picked Mario Kart Wii ROM (ISO/RVZ/WBFS) URI by
 * resolving the content URI to a real filesystem path, reading up to
 * 4 KiB of header, and running the magic-byte / game-id checks in
 * [GameTypeParser]. Pure utility: no Compose, no Android lifecycle.
 */
object IsoValidator {

  /** Maximum number of header bytes needed to validate ISO/RVZ/WBFS/WAD formats. */
  private const val HEADER_READ_SIZE = 4096

  /**
   * Validates [uri] in one shot. Returns the parsed [GameInfo] when the
   * file is a recognised Mario Kart Wii disc, otherwise a failure
   * variant describing why it was rejected.
   */
  fun validate(context: Context, uri: Uri): IsoValidationResult {
    val path = PackStorage.resolveContentUriToPath(uri) ?: return IsoValidationResult.Unreachable
    val file = File(path)
    if (!file.exists()) return IsoValidationResult.NotFound
    val info = parseHeader(file) ?: return IsoValidationResult.Invalid
    return if (info.format == GameFormat.Invalid) {
      IsoValidationResult.Invalid
    } else {
      IsoValidationResult.Valid(info)
    }
  }

  /**
   * Reads the file header and returns the parsed [GameInfo], or null
   * if the file could not be read. Returns a [GameInfo] with
   * [GameFormat.Invalid] when the format is unrecognised.
   */
  fun parseHeader(file: File): GameInfo? {
    return runCatching {
          val header = file.inputStream().use { input ->
            val buf = ByteArray(HEADER_READ_SIZE)
            val bytesRead = input.read(buf)
            if (bytesRead <= 0) ByteArray(0) else buf.copyOf(bytesRead)
          }
          GameTypeParser.parseGameInfo(file.name, header)
        }
        .getOrNull()
  }
}
