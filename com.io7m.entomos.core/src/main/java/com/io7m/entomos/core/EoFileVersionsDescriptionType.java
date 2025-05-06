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
import com.io7m.verona.core.Version;
import org.immutables.value.Value;

import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * A set of descriptions of each version of a file format.
 */

@ImmutablesStyleType
@Value.Immutable
public interface EoFileVersionsDescriptionType
{
  /**
   * @return The description of each format version
   */

  List<EoFileDescription> descriptions();

  /**
   * @return The versions
   */

  @Value.Lazy
  default NavigableMap<Version, EoFileDescription> versions()
  {
    final var m = new TreeMap<Version, EoFileDescription>();
    for (final var d : this.descriptions()) {
      final var version =
        Version.of(d.versionMajor(), d.versionMinor(), 0);
      m.put(version, d);
    }
    return Collections.unmodifiableNavigableMap(m);
  }

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    final var descriptions = this.descriptions();
    if (descriptions.isEmpty()) {
      throw new IllegalArgumentException(
        "Must specify at least one file version description."
      );
    }

    final var fileVersions =
      descriptions
        .stream()
        .map(x -> Version.of(x.versionMajor(), x.versionMinor(), 0))
        .collect(Collectors.toUnmodifiableSet());

    if (fileVersions.size() != descriptions.size()) {
      throw new IllegalArgumentException(
        "File format versions must be unique."
      );
    }

    final var fileTags =
      descriptions
        .stream()
        .map(EoFileDescription::fileTag)
        .collect(Collectors.toUnmodifiableSet());

    if (fileTags.size() != 1) {
      throw new IllegalArgumentException(
        "All file format versions must use the same file tag."
      );
    }

    final var endTags =
      descriptions
        .stream()
        .map(EoFileDescription::endTag)
        .collect(Collectors.toUnmodifiableSet());

    if (endTags.size() != 1) {
      throw new IllegalArgumentException(
        "All file format versions must use the same end tag."
      );
    }
  }
}
