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
import com.io7m.entomos.core.EoFileVersionsDescription;
import com.io7m.seltzer.slf4j.SSLogging;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class EoFileVersionsDescriptionTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EoFileVersionsDescriptionTest.class);

  private static final long TAG_FILE =
    0x10101010_20202020L;
  private static final long TAG_END =
    0x20202020_30303030L;
  private static final long TAG_A =
    0xAAAAAAAA_AAAAAAAAL;
  private static final long TAG_B =
    0xBBBBBBBB_BBBBBBBBL;
  private static final long TAG_C =
    0xCCCCCCCC_CCCCCCCCL;

  @Test
  public void testEmpty()
  {
    final var ex =
      assertThrows(
        IllegalArgumentException.class, () -> {
          EoFileVersionsDescription.builder()
            .build();
        });

    SSLogging.logMDC(LOG, Level.DEBUG, EoException.wrap(ex));
    assertEquals("Must specify at least one file version description.", ex.getMessage());
  }

  @Test
  public void testVersionDuplicate()
  {
    final var v1 =
      EoFileDescription.builder()
        .setVersionMajor(1)
        .setVersionMinor(0)
        .setFileTag(TAG_FILE)
        .setEndTag(TAG_END)
        .addSections(
          EoFileSectionDescription.builder()
            .setTag(TAG_A)
            .build()
        ).build();

    final var ex =
      assertThrows(
        IllegalArgumentException.class, () -> {
          EoFileVersionsDescription.builder()
            .addDescriptions(v1, v1)
            .build();
        });

    SSLogging.logMDC(LOG, Level.DEBUG, EoException.wrap(ex));
    assertEquals("File format versions must be unique.", ex.getMessage());
  }

  @Test
  public void testVersionWrongFileTag()
  {
    final var v1 =
      EoFileDescription.builder()
        .setVersionMajor(1)
        .setVersionMinor(0)
        .setFileTag(TAG_FILE)
        .setEndTag(TAG_END)
        .addSections(
          EoFileSectionDescription.builder()
            .setTag(TAG_A)
            .build()
        ).build();

    final var v2 =
      EoFileDescription.builder()
        .setVersionMajor(2)
        .setVersionMinor(0)
        .setFileTag(0x3030)
        .setEndTag(TAG_END)
        .addSections(
          EoFileSectionDescription.builder()
            .setTag(TAG_A)
            .build()
        ).build();

    final var ex =
      assertThrows(
        IllegalArgumentException.class, () -> {
          EoFileVersionsDescription.builder()
            .addDescriptions(v1, v2)
            .build();
        });

    SSLogging.logMDC(LOG, Level.DEBUG, EoException.wrap(ex));
    assertEquals("All file format versions must use the same file tag.", ex.getMessage());
  }

  @Test
  public void testVersionWrongEndTag()
  {
    final var v1 =
      EoFileDescription.builder()
        .setVersionMajor(1)
        .setVersionMinor(0)
        .setFileTag(TAG_FILE)
        .setEndTag(TAG_END)
        .addSections(
          EoFileSectionDescription.builder()
            .setTag(TAG_A)
            .build()
        ).build();

    final var v2 =
      EoFileDescription.builder()
        .setVersionMajor(2)
        .setVersionMinor(0)
        .setFileTag(TAG_FILE)
        .setEndTag(0x3030)
        .addSections(
          EoFileSectionDescription.builder()
            .setTag(TAG_A)
            .build()
        ).build();

    final var ex =
      assertThrows(
        IllegalArgumentException.class, () -> {
          EoFileVersionsDescription.builder()
            .addDescriptions(v1, v2)
            .build();
        });

    SSLogging.logMDC(LOG, Level.DEBUG, EoException.wrap(ex));
    assertEquals("All file format versions must use the same end tag.", ex.getMessage());
  }
}
