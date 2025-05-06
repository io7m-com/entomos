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

import com.io7m.verona.core.Version;

import java.nio.channels.SeekableByteChannel;
import java.util.NavigableSet;

/**
 * A file reader.
 */

public interface EoFileReaderType
  extends AutoCloseable
{
  /**
   * @return A read-only set of the sections present in the file
   */

  NavigableSet<EoFileSection> sections();

  /**
   * @return The file tag
   */

  long fileTag();

  /**
   * @return The version of the file format in this file
   */

  Version version();

  /**
   * Get access to the data within a section on the file.
   *
   * @param section The section
   *
   * @return A byte channel for the section data
   */

  SeekableByteChannel dataChannel(
    EoFileSection section)
    throws EoException;

  @Override
  void close()
    throws EoException;
}
