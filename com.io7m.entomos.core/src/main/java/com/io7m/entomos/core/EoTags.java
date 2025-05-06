/*
 * Copyright © 2025 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package com.io7m.entomos.core;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Convenience functions to declare tags.
 */

public final class EoTags
{
  private EoTags()
  {

  }

  /**
   * Create a PNG-style tag.
   *
   * @param b0 The first byte of the tag (such as 'P')
   * @param b1 The second byte of the tag (such as 'N')
   * @param b2 The third byte of the tag (such as 'G')
   *
   * @return The compiled tag
   */

  public static long pngStyle(
    final byte b0,
    final byte b1,
    final byte b2)
  {
    final var bytes =
      new byte[8];
    final var buffer =
      ByteBuffer.wrap(bytes);

    buffer.order(ByteOrder.BIG_ENDIAN);
    bytes[0] = (byte) 0x89;
    bytes[1] = b0;
    bytes[2] = b1;
    bytes[3] = b2;
    bytes[4] = 0x0D;
    bytes[5] = 0x0A;
    bytes[6] = 0x1A;
    bytes[7] = 0x0A;
    return buffer.getLong(0);
  }

  /**
   * Create a tag from humanly-readable characters.
   *
   * @param text The text
   *
   * @return On errors
   *
   * @throws IllegalArgumentException If the string is not eight octets
   */

  public static long ofString(
    final String text)
  {
    final var bytes =
      text.getBytes(StandardCharsets.US_ASCII);

    if (bytes.length != 8) {
      throw new IllegalArgumentException(
        "The given string must reduce to 8 ASCII bytes (received %s)"
          .formatted(bytes.length)
      );
    }

    final var buffer = ByteBuffer.wrap(bytes);
    buffer.order(ByteOrder.BIG_ENDIAN);
    return buffer.getLong(0);
  }
}
