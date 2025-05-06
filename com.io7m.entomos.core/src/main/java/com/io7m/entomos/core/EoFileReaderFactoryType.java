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

import java.io.IOException;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;

/**
 * A factory of file readers.
 *
 * @param <P> The type of extra parameters
 */

public interface EoFileReaderFactoryType<P>
{
  /**
   * Open a reader for the given readable channel.
   *
   * @param uri        The URI for diagnostic purposes
   * @param fileTag    The required file tag
   * @param endTag     The required end tag
   * @param channel    The channel
   * @param parameters The extra parameters
   *
   * @return A reader
   *
   * @throws EoException On errors
   */

  EoFileReaderType forChannel(
    URI uri,
    long fileTag,
    long endTag,
    SeekableByteChannel channel,
    P parameters)
    throws EoException;

  /**
   * Open a reader for the given file.
   *
   * @param file       The file
   * @param fileTag    The required file tag
   * @param endTag     The required end tag
   * @param parameters The extra parameters
   *
   * @return A reader
   *
   * @throws EoException On errors
   */

  default EoFileReaderType forFile(
    final long fileTag,
    final long endTag,
    final Path file,
    final P parameters)
    throws EoException
  {
    try {
      return this.forChannel(
        file.toUri(),
        fileTag,
        endTag,
        FileChannel.open(file),
        parameters
      );
    } catch (final IOException e) {
      throw EoException.wrap(e);
    }
  }
}
