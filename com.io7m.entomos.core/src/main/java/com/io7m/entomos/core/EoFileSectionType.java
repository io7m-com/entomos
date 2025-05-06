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

import com.io7m.immutables.styles.ImmutablesStyleType;
import org.immutables.value.Value;

import java.util.Comparator;

/**
 * A section within an open file.
 */

@ImmutablesStyleType
@Value.Immutable
public sealed interface EoFileSectionType
  extends Comparable<EoFileSection>
  permits EoFileSection
{
  /**
   * A comparator that compares file section by offsets.
   */

  Comparator<EoFileSection> EO_OFFSET_COMPARATOR =
    (o1, o2) -> Long.compareUnsigned(o1.offset(), o2.offset());

  /**
   * @return The section tag
   */

  long tag();

  /**
   * @return The size of the data in the section, minus any padding
   */

  long dataSize();

  /**
   * @return The offset of the section within the file
   */

  long offset();

  /**
   * @return The offset of the section data within the file
   */

  default long dataOffset()
  {
    return this.offset() + 16L;
  }

  @Override
  default int compareTo(
    final EoFileSection other)
  {
    return EO_OFFSET_COMPARATOR.compare((EoFileSection) this, other);
  }
}
