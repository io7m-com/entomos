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


package com.io7m.entomos.tests;

import com.io7m.entomos.core.EoException;
import com.io7m.entomos.core.EoFileDescription;
import com.io7m.entomos.core.EoFileSectionDescription;
import com.io7m.entomos.core.EoSectionOrdering;
import com.io7m.seltzer.slf4j.SSLogging;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class EoFileDescriptionTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EoFileDescriptionTest.class);

  @Test
  public void testMultipleFirsts()
  {
    final var ex =
      assertThrows(
        IllegalArgumentException.class, () -> {
          EoFileDescription.builder()
            .setFileTag(0x0)
            .setEndTag(0x1)
            .setVersionMajor(1)
            .setVersionMinor(0)
            .addSections(
              EoFileSectionDescription.builder()
                .setTag(2L)
                .setOrdering(EoSectionOrdering.MUST_BE_FIRST)
                .build()
            )
            .addSections(
              EoFileSectionDescription.builder()
                .setTag(3L)
                .setOrdering(EoSectionOrdering.MUST_BE_FIRST)
                .build()
            )
            .build();
        });

    SSLogging.logMDC(LOG, Level.DEBUG, EoException.wrap(ex));
    assertTrue(ex.getMessage().contains("one section can be marked as being 'first'"));
  }

  @Test
  public void testMultipleLasts()
  {
    final var ex =
      assertThrows(
        IllegalArgumentException.class, () -> {
          EoFileDescription.builder()
            .setFileTag(0x0)
            .setEndTag(0x1)
            .setVersionMajor(1)
            .setVersionMinor(0)
            .addSections(
              EoFileSectionDescription.builder()
                .setTag(2L)
                .setOrdering(EoSectionOrdering.MUST_BE_LAST)
                .build()
            )
            .addSections(
              EoFileSectionDescription.builder()
                .setTag(3L)
                .setOrdering(EoSectionOrdering.MUST_BE_LAST)
                .build()
            )
            .build();
        });

    SSLogging.logMDC(LOG, Level.DEBUG, EoException.wrap(ex));
    assertTrue(ex.getMessage().contains("one section can be marked as being 'last'"));
  }

  @Test
  public void testTagsDuplicate()
  {
    final var ex =
      assertThrows(
        IllegalArgumentException.class, () -> {
          EoFileDescription.builder()
            .setFileTag(0x0)
            .setEndTag(0x1)
            .setVersionMajor(1)
            .setVersionMinor(0)
            .addSections(
              EoFileSectionDescription.builder()
                .setTag(2L)
                .build()
            )
            .addSections(
              EoFileSectionDescription.builder()
                .setTag(2L)
                .build()
            )
            .build();
        });

    SSLogging.logMDC(LOG, Level.DEBUG, EoException.wrap(ex));
    assertTrue(ex.getMessage().contains("Section tags must be unique (2 is not)"));
  }

  @Test
  public void testTagMatchesFile()
  {
    final var ex =
      assertThrows(
        IllegalArgumentException.class, () -> {
          EoFileDescription.builder()
            .setFileTag(0x0)
            .setEndTag(0x1)
            .setVersionMajor(1)
            .setVersionMinor(0)
            .addSections(
              EoFileSectionDescription.builder()
                .setTag(0L)
                .build()
            )
            .build();
        });

    SSLogging.logMDC(LOG, Level.DEBUG, EoException.wrap(ex));
    assertTrue(ex.getMessage().contains("The file tag cannot also be used a section tag."));
  }

  @Test
  public void testTagMatchesEnd()
  {
    final var ex =
      assertThrows(
        IllegalArgumentException.class, () -> {
          EoFileDescription.builder()
            .setFileTag(0x0)
            .setEndTag(0x1)
            .setVersionMajor(1)
            .setVersionMinor(0)
            .addSections(
              EoFileSectionDescription.builder()
                .setTag(1L)
                .build()
            )
            .build();
        });

    SSLogging.logMDC(LOG, Level.DEBUG, EoException.wrap(ex));
    assertTrue(ex.getMessage().contains("The end tag cannot also be used a section tag."));
  }

  @Test
  public void testTagMatchesFileEnd()
  {
    final var ex =
      assertThrows(
        IllegalArgumentException.class, () -> {
          EoFileDescription.builder()
            .setFileTag(0x0)
            .setEndTag(0x0)
            .setVersionMajor(1)
            .setVersionMinor(0)
            .addSections(
              EoFileSectionDescription.builder()
                .setTag(1L)
                .build()
            )
            .build();
        });

    SSLogging.logMDC(LOG, Level.DEBUG, EoException.wrap(ex));
    assertTrue(ex.getMessage().contains("The file tag and end tag must be different."));
  }
}
