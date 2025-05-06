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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * The description of a file format.
 */

@ImmutablesStyleType
@Value.Immutable
public interface EoFileDescriptionType
{
  /**
   * @return The file tag
   */

  long fileTag();

  /**
   * @return The file format major version
   */

  int versionMajor();

  /**
   * @return The file format minor version
   */

  int versionMinor();

  /**
   * @return The list of section descriptions
   */

  List<EoFileSectionDescription> sections();

  /**
   * @return The tag of the mandatory end section
   */

  long endTag();

  /**
   * @return A specification of what to do about unknown sections.
   */

  @Value.Default
  default EoSectionsUnknown sectionsUnknown()
  {
    return EoSectionsUnknown.UNKNOWN_SECTIONS_NOT_PERMITTED;
  }

  /**
   * @return The section descriptions by tag
   */

  @Value.Lazy
  default NavigableMap<Long, EoFileSectionDescription> sectionByTag()
  {
    final var m = new TreeMap<Long, EoFileSectionDescription>();
    for (final var d : this.sections()) {
      m.put(d.tag(), d);
    }
    return Collections.unmodifiableNavigableMap(m);
  }

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    final var sectionList =
      this.sections();
    final var sectionMap =
      new HashMap<Long, EoFileSectionDescription>(sectionList.size());

    final var firsts =
      sectionList.stream()
        .filter(s -> s.ordering() == EoSectionOrdering.MUST_BE_FIRST)
        .count();

    if (firsts > 1) {
      throw new IllegalArgumentException(
        "At most one section can be marked as being 'first'"
      );
    }

    final var lasts =
      sectionList.stream()
        .filter(s -> s.ordering() == EoSectionOrdering.MUST_BE_LAST)
        .count();

    if (lasts > 1) {
      throw new IllegalArgumentException(
        "At most one section can be marked as being 'last'"
      );
    }

    for (final var section : sectionList) {
      if (sectionMap.containsKey(section.tag())) {
        throw new IllegalArgumentException(
          "Section tags must be unique (%s is not)"
            .formatted(Long.toUnsignedString(section.tag()))
        );
      }
      sectionMap.put(section.tag(), section);
    }

    if (sectionMap.containsKey(this.fileTag())) {
      throw new IllegalArgumentException(
        "The file tag cannot also be used a section tag."
      );
    }

    if (sectionMap.containsKey(this.endTag())) {
      throw new IllegalArgumentException(
        "The end tag cannot also be used a section tag."
      );
    }

    if (this.fileTag() == this.endTag()) {
      throw new IllegalArgumentException(
        "The file tag and end tag must be different."
      );
    }
  }
}
