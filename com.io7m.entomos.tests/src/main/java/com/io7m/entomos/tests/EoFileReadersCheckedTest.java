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
import com.io7m.entomos.core.EoFileReadersChecked;
import com.io7m.entomos.core.EoFileReadersUnchecked;
import com.io7m.entomos.core.EoFileSection;
import com.io7m.entomos.core.EoFileSectionDescription;
import com.io7m.entomos.core.EoFileVersionsDescription;
import com.io7m.entomos.core.EoSectionCardinality;
import com.io7m.entomos.core.EoSectionOrdering;
import com.io7m.entomos.core.EoSectionsUnknown;
import com.io7m.jbssio.vanilla.BSSWriters;
import com.io7m.seltzer.slf4j.SSLogging;
import com.io7m.verona.core.Version;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.io7m.entomos.core.EoSectionCardinality.ONE;
import static com.io7m.entomos.core.EoSectionCardinality.ONE_TO_N;
import static com.io7m.entomos.core.EoSectionCardinality.ZERO_TO_ONE;
import static com.io7m.entomos.core.EoSectionOrdering.MUST_BE_FIRST;
import static com.io7m.entomos.core.EoSectionOrdering.MUST_BE_LAST;
import static com.io7m.entomos.core.EoSectionsUnknown.UNKNOWN_SECTIONS_NOT_PERMITTED;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class EoFileReadersCheckedTest
{
  private static final Logger LOG =
    LoggerFactory.getLogger(EoFileReadersCheckedTest.class);

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

  private EoFileReadersChecked readers;
  private BSSWriters bssWriters;
  private EoFileDescription format1_0;
  private EoFileVersionsDescription formats;

  @BeforeEach
  public void setup()
  {
    this.readers =
      new EoFileReadersChecked();
    this.bssWriters =
      new BSSWriters();

    this.format1_0 =
      EoFileDescription.builder()
        .setVersionMajor(1)
        .setVersionMinor(0)
        .setFileTag(TAG_FILE)
        .setEndTag(TAG_END)
        .addSections(
          EoFileSectionDescription.builder()
            .setTag(TAG_A)
            .setCardinality(ONE)
            .setOrdering(MUST_BE_FIRST)
            .build(),
          EoFileSectionDescription.builder()
            .setTag(TAG_B)
            .setCardinality(ONE_TO_N)
            .build(),
          EoFileSectionDescription.builder()
            .setTag(TAG_C)
            .setCardinality(ZERO_TO_ONE)
            .setOrdering(MUST_BE_LAST)
            .build()
        ).build();

    this.formats =
      EoFileVersionsDescription.builder()
        .addDescriptions(this.format1_0)
        .build();
  }

  @Test
  public void testFileBrokenChannel(
    final @TempDir Path directory)
    throws Exception
  {
    final var file =
      directory.resolve("file.bin");

    Files.write(file, new byte[0]);

    final var ex =
      assertThrows(
        EoException.class, () -> {
          this.readers.forChannel(
            URI.create("urn:x"),
            TAG_FILE,
            TAG_END,
            brokenChannel(),
            formats
          );
        });

    SSLogging.logMDC(LOG, Level.DEBUG, ex);
    assertEquals("error-resources", ex.errorCode());
  }

  @Test
  public void testFileTagMissing(
    final @TempDir Path directory)
    throws Exception
  {
    final var file =
      directory.resolve("file.bin");

    Files.write(file, new byte[0]);

    final var ex =
      assertThrows(
        EoException.class, () -> {
          this.readers.forFile(TAG_FILE, TAG_END, file, formats);
        });

    SSLogging.logMDC(LOG, Level.DEBUG, ex);
    assertEquals("error-file-tag-missing", ex.errorCode());
  }

  @Test
  public void testFileTagWrong(
    final @TempDir Path directory)
    throws Exception
  {
    final var file =
      directory.resolve("file.bin");

    try (final var channel = FileChannel.open(file, CREATE, WRITE)) {
      try (final var writer =
             this.bssWriters.createWriterFromChannel(
               file.toUri(),
               channel,
               "File")) {
        writer.writeU64BE(0x23L);
      }
    }

    final var ex =
      assertThrows(
        EoException.class, () -> {
          this.readers.forFile(TAG_FILE, TAG_END, file, formats);
        });

    SSLogging.logMDC(LOG, Level.DEBUG, ex);
    assertEquals("error-file-tag-incorrect", ex.errorCode());
  }

  @Test
  public void testFileTagVersionMajorMissing(
    final @TempDir Path directory)
    throws Exception
  {
    final var file =
      directory.resolve("file.bin");

    try (final var channel = FileChannel.open(file, CREATE, WRITE)) {
      try (final var writer =
             this.bssWriters.createWriterFromChannel(
               file.toUri(),
               channel,
               "File")) {
        writer.writeU64BE(TAG_FILE);
      }
    }

    final var ex =
      assertThrows(
        EoException.class, () -> {
          this.readers.forFile(TAG_FILE, TAG_END, file, formats);
        });

    SSLogging.logMDC(LOG, Level.DEBUG, ex);
    assertEquals("error-file-version-major-missing", ex.errorCode());
  }

  @Test
  public void testFileTagVersionMinorMissing(
    final @TempDir Path directory)
    throws Exception
  {
    final var file =
      directory.resolve("file.bin");

    try (final var channel = FileChannel.open(file, CREATE, WRITE)) {
      try (final var writer =
             this.bssWriters.createWriterFromChannel(
               file.toUri(),
               channel,
               "File")) {
        writer.writeU64BE(TAG_FILE);
        writer.writeU32BE(1L);
      }
    }

    final var ex =
      assertThrows(
        EoException.class, () -> {
          this.readers.forFile(TAG_FILE, TAG_END, file, formats);
        });

    SSLogging.logMDC(LOG, Level.DEBUG, ex);
    assertEquals("error-file-version-minor-missing", ex.errorCode());
  }

  @Test
  public void testFileTagsOK(
    final @TempDir Path directory)
    throws Exception
  {
    final var file =
      directory.resolve("file.bin");

    try (final var channel = FileChannel.open(file, CREATE, WRITE)) {
      try (final var writer =
             this.bssWriters.createWriterFromChannel(
               file.toUri(),
               channel,
               "File")) {
        writer.writeU64BE(TAG_FILE);
        writer.writeU32BE(1L);
        writer.writeU32BE(0L);

        writer.writeU64BE(TAG_A);
        writer.writeU64BE(12L);
        writer.writeBytes(new byte[16]);

        writer.writeU64BE(TAG_B);
        writer.writeU64BE(4L);
        writer.writeBytes(new byte[16]);

        writer.writeU64BE(TAG_C);
        writer.writeU64BE(6L);
        writer.writeBytes(new byte[16]);

        writer.writeU64BE(TAG_END);
        writer.writeU64BE(0L);
      }
    }

    try (final var reader = this.readers.forFile(TAG_FILE, TAG_END, file, formats)) {
      assertEquals(TAG_FILE, reader.fileTag());
      assertEquals(Version.of(1, 0, 0), reader.version());

      final var iter = reader.sections().iterator();

      final var s0 = iter.next();
      assertEquals(TAG_A, s0.tag());
      assertEquals(12L, s0.dataSize());
      assertEquals(16L, s0.offset());
      assertEquals(16 + 16, s0.dataOffset());
      try (final var channel = reader.dataChannel(s0)) {
        assertEquals(12L, channel.size());
      }

      final var s1 = iter.next();
      assertEquals(TAG_B, s1.tag());
      assertEquals(4L, s1.dataSize());
      assertEquals(48L, s1.offset());
      assertEquals(48 + 16, s1.dataOffset());
      try (final var channel = reader.dataChannel(s1)) {
        assertEquals(4L, channel.size());
      }

      final var s2 = iter.next();
      assertEquals(TAG_C, s2.tag());
      assertEquals(6L, s2.dataSize());
      assertEquals(80L, s2.offset());
      assertEquals(80 + 16, s2.dataOffset());
      try (final var channel = reader.dataChannel(s2)) {
        assertEquals(6L, channel.size());
      }

      final var s3 = iter.next();
      assertEquals(TAG_END, s3.tag());
      assertEquals(0L, s3.dataSize());
      assertEquals(112L, s3.offset());
      assertEquals(112 + 16, s3.dataOffset());
      try (final var channel = reader.dataChannel(s3)) {
        assertEquals(0L, channel.size());
      }

      final var ex =
        assertThrows(EoException.class, () -> {
          reader.dataChannel(
            EoFileSection.builder()
              .setTag(0L)
              .setOffset(0L)
              .setDataSize(0L)
              .build()
          );
        });

      assertEquals("error-file-section-not-present", ex.errorCode());
    }
  }

  @Test
  public void testFileVersionUnsupported(
    final @TempDir Path directory)
    throws Exception
  {
    final var file =
      directory.resolve("file.bin");

    try (final var channel = FileChannel.open(file, CREATE, WRITE)) {
      try (final var writer =
             this.bssWriters.createWriterFromChannel(
               file.toUri(),
               channel,
               "File")) {
        writer.writeU64BE(TAG_FILE);
        writer.writeU32BE(999L);
        writer.writeU32BE(0L);

        writer.writeU64BE(TAG_A);
        writer.writeU64BE(12L);
        writer.writeBytes(new byte[16]);

        writer.writeU64BE(TAG_B);
        writer.writeU64BE(4L);
        writer.writeBytes(new byte[16]);

        writer.writeU64BE(TAG_C);
        writer.writeU64BE(6L);
        writer.writeBytes(new byte[16]);

        writer.writeU64BE(TAG_END);
        writer.writeU64BE(0L);
      }
    }

    final var ex =
      assertThrows(
        EoException.class, () -> {
          this.readers.forFile(TAG_FILE, TAG_END, file, formats);
        });

    SSLogging.logMDC(LOG, Level.DEBUG, ex);
    assertEquals("error-file-version-not-supported", ex.errorCode());
  }

  @Test
  public void testFileEndMissing(
    final @TempDir Path directory)
    throws Exception
  {
    final var file =
      directory.resolve("file.bin");

    try (final var channel = FileChannel.open(file, CREATE, WRITE)) {
      try (final var writer =
             this.bssWriters.createWriterFromChannel(
               file.toUri(),
               channel,
               "File")) {
        writer.writeU64BE(TAG_FILE);
        writer.writeU32BE(1L);
        writer.writeU32BE(0L);
      }
    }

    final var ex =
      assertThrows(
        EoException.class, () -> {
          this.readers.forFile(TAG_FILE, TAG_END, file, formats);
        });

    SSLogging.logMDC(LOG, Level.DEBUG, ex);
    assertEquals("error-file-end-missing", ex.errorCode());
  }

  @Test
  public void testFileTagCardinalityTooManyOne(
    final @TempDir Path directory)
    throws Exception
  {
    final var file =
      directory.resolve("file.bin");

    this.format1_0 =
      EoFileDescription.builder()
        .setVersionMajor(1)
        .setVersionMinor(0)
        .setFileTag(TAG_FILE)
        .setEndTag(TAG_END)
        .addSections(
          EoFileSectionDescription.builder()
            .setTag(TAG_A)
            .setCardinality(ONE)
            .build()
        ).build();

    this.formats =
      EoFileVersionsDescription.builder()
        .addDescriptions(this.format1_0)
        .build();

    try (final var channel = FileChannel.open(file, CREATE, WRITE)) {
      try (final var writer =
             this.bssWriters.createWriterFromChannel(
               file.toUri(),
               channel,
               "File")) {
        writer.writeU64BE(TAG_FILE);
        writer.writeU32BE(1L);
        writer.writeU32BE(0L);

        writer.writeU64BE(TAG_A);
        writer.writeU64BE(12L);
        writer.writeBytes(new byte[16]);

        writer.writeU64BE(TAG_A);
        writer.writeU64BE(12L);
        writer.writeBytes(new byte[16]);

        writer.writeU64BE(TAG_END);
        writer.writeU64BE(0L);
      }
    }

    final var ex =
      assertThrows(
        EoException.class, () -> {
          this.readers.forFile(TAG_FILE, TAG_END, file, formats);
        });

    SSLogging.logMDC(LOG, Level.DEBUG, ex);
    assertEquals("error-section-tag-cardinality", ex.errorCode());
  }

  @Test
  public void testFileTagCardinalityTooManyZeroOne(
    final @TempDir Path directory)
    throws Exception
  {
    final var file =
      directory.resolve("file.bin");

    this.format1_0 =
      EoFileDescription.builder()
        .setVersionMajor(1)
        .setVersionMinor(0)
        .setFileTag(TAG_FILE)
        .setEndTag(TAG_END)
        .addSections(
          EoFileSectionDescription.builder()
            .setTag(TAG_A)
            .setCardinality(ZERO_TO_ONE)
            .build()
        ).build();

    this.formats =
      EoFileVersionsDescription.builder()
        .addDescriptions(this.format1_0)
        .build();

    try (final var channel = FileChannel.open(file, CREATE, WRITE)) {
      try (final var writer =
             this.bssWriters.createWriterFromChannel(
               file.toUri(),
               channel,
               "File")) {
        writer.writeU64BE(TAG_FILE);
        writer.writeU32BE(1L);
        writer.writeU32BE(0L);

        writer.writeU64BE(TAG_A);
        writer.writeU64BE(12L);
        writer.writeBytes(new byte[16]);

        writer.writeU64BE(TAG_A);
        writer.writeU64BE(12L);
        writer.writeBytes(new byte[16]);

        writer.writeU64BE(TAG_END);
        writer.writeU64BE(0L);
      }
    }

    final var ex =
      assertThrows(
        EoException.class, () -> {
          this.readers.forFile(TAG_FILE, TAG_END, file, formats);
        });

    SSLogging.logMDC(LOG, Level.DEBUG, ex);
    assertEquals("error-section-tag-cardinality", ex.errorCode());
  }

  @Test
  public void testFileTagCardinalityTooFewOneN(
    final @TempDir Path directory)
    throws Exception
  {
    final var file =
      directory.resolve("file.bin");

    this.format1_0 =
      EoFileDescription.builder()
        .setVersionMajor(1)
        .setVersionMinor(0)
        .setFileTag(TAG_FILE)
        .setEndTag(TAG_END)
        .addSections(
          EoFileSectionDescription.builder()
            .setTag(TAG_A)
            .setCardinality(ONE_TO_N)
            .build()
        ).build();

    this.formats =
      EoFileVersionsDescription.builder()
        .addDescriptions(this.format1_0)
        .build();

    try (final var channel = FileChannel.open(file, CREATE, WRITE)) {
      try (final var writer =
             this.bssWriters.createWriterFromChannel(
               file.toUri(),
               channel,
               "File")) {
        writer.writeU64BE(TAG_FILE);
        writer.writeU32BE(1L);
        writer.writeU32BE(0L);

        writer.writeU64BE(TAG_END);
        writer.writeU64BE(0L);
      }
    }

    final var ex =
      assertThrows(
        EoException.class, () -> {
          this.readers.forFile(TAG_FILE, TAG_END, file, formats);
        });

    SSLogging.logMDC(LOG, Level.DEBUG, ex);
    assertEquals("error-section-tag-cardinality", ex.errorCode());
  }

  @Test
  public void testFileTagMultipleEnds(
    final @TempDir Path directory)
    throws Exception
  {
    final var file =
      directory.resolve("file.bin");

    this.format1_0 =
      EoFileDescription.builder()
        .setVersionMajor(1)
        .setVersionMinor(0)
        .setFileTag(TAG_FILE)
        .setEndTag(TAG_END)
        .build();

    this.formats =
      EoFileVersionsDescription.builder()
        .addDescriptions(this.format1_0)
        .build();

    try (final var channel = FileChannel.open(file, CREATE, WRITE)) {
      try (final var writer =
             this.bssWriters.createWriterFromChannel(
               file.toUri(),
               channel,
               "File")) {
        writer.writeU64BE(TAG_FILE);
        writer.writeU32BE(1L);
        writer.writeU32BE(0L);

        writer.writeU64BE(TAG_END);
        writer.writeU64BE(0L);

        writer.writeU64BE(TAG_END);
        writer.writeU64BE(0L);
      }
    }

    try (final var reader =
           this.readers.forFile(TAG_FILE, TAG_END, file, this.formats)) {
      assertEquals(1, reader.sections().size());
    }
  }

  @Test
  public void testFileTagOrderingFirst(
    final @TempDir Path directory)
    throws Exception
  {
    final var file =
      directory.resolve("file.bin");

    this.format1_0 =
      EoFileDescription.builder()
        .setVersionMajor(1)
        .setVersionMinor(0)
        .setFileTag(TAG_FILE)
        .setEndTag(TAG_END)
        .addSections(
          EoFileSectionDescription.builder()
            .setTag(TAG_A)
            .setOrdering(MUST_BE_FIRST)
            .build(),
          EoFileSectionDescription.builder()
            .setTag(TAG_B)
            .build()
        ).build();

    this.formats =
      EoFileVersionsDescription.builder()
        .addDescriptions(this.format1_0)
        .build();

    try (final var channel = FileChannel.open(file, CREATE, WRITE)) {
      try (final var writer =
             this.bssWriters.createWriterFromChannel(
               file.toUri(),
               channel,
               "File")) {
        writer.writeU64BE(TAG_FILE);
        writer.writeU32BE(1L);
        writer.writeU32BE(0L);

        writer.writeU64BE(TAG_B);
        writer.writeU64BE(0L);

        writer.writeU64BE(TAG_A);
        writer.writeU64BE(0L);

        writer.writeU64BE(TAG_END);
        writer.writeU64BE(0L);
      }
    }

    final var ex =
      assertThrows(
        EoException.class, () -> {
          this.readers.forFile(TAG_FILE, TAG_END, file, formats);
        });

    SSLogging.logMDC(LOG, Level.DEBUG, ex);
    assertEquals("error-section-tag-first", ex.errorCode());
  }

  @Test
  public void testFileTagOrderingLast(
    final @TempDir Path directory)
    throws Exception
  {
    final var file =
      directory.resolve("file.bin");

    this.format1_0 =
      EoFileDescription.builder()
        .setVersionMajor(1)
        .setVersionMinor(0)
        .setFileTag(TAG_FILE)
        .setEndTag(TAG_END)
        .addSections(
          EoFileSectionDescription.builder()
            .setTag(TAG_A)
            .setOrdering(MUST_BE_LAST)
            .build(),
          EoFileSectionDescription.builder()
            .setTag(TAG_B)
            .build()
        ).build();

    this.formats =
      EoFileVersionsDescription.builder()
        .addDescriptions(this.format1_0)
        .build();

    try (final var channel = FileChannel.open(file, CREATE, WRITE)) {
      try (final var writer =
             this.bssWriters.createWriterFromChannel(
               file.toUri(),
               channel,
               "File")) {
        writer.writeU64BE(TAG_FILE);
        writer.writeU32BE(1L);
        writer.writeU32BE(0L);

        writer.writeU64BE(TAG_A);
        writer.writeU64BE(0L);

        writer.writeU64BE(TAG_B);
        writer.writeU64BE(0L);

        writer.writeU64BE(TAG_END);
        writer.writeU64BE(0L);
      }
    }

    final var ex =
      assertThrows(
        EoException.class, () -> {
          this.readers.forFile(TAG_FILE, TAG_END, file, formats);
        });

    SSLogging.logMDC(LOG, Level.DEBUG, ex);
    assertEquals("error-section-tag-last", ex.errorCode());
  }

  @Test
  public void testFileTagUnknown(
    final @TempDir Path directory)
    throws Exception
  {
    final var file =
      directory.resolve("file.bin");

    this.format1_0 =
      EoFileDescription.builder()
        .setVersionMajor(1)
        .setVersionMinor(0)
        .setFileTag(TAG_FILE)
        .setEndTag(TAG_END)
        .setSectionsUnknown(UNKNOWN_SECTIONS_NOT_PERMITTED)
        .build();

    this.formats =
      EoFileVersionsDescription.builder()
        .addDescriptions(this.format1_0)
        .build();

    try (final var channel = FileChannel.open(file, CREATE, WRITE)) {
      try (final var writer =
             this.bssWriters.createWriterFromChannel(
               file.toUri(),
               channel,
               "File")) {
        writer.writeU64BE(TAG_FILE);
        writer.writeU32BE(1L);
        writer.writeU32BE(0L);

        writer.writeU64BE(TAG_A);
        writer.writeU64BE(0L);

        writer.writeU64BE(TAG_B);
        writer.writeU64BE(0L);

        writer.writeU64BE(TAG_END);
        writer.writeU64BE(0L);
      }
    }

    final var ex =
      assertThrows(
        EoException.class, () -> {
          this.readers.forFile(TAG_FILE, TAG_END, file, formats);
        });

    SSLogging.logMDC(LOG, Level.DEBUG, ex);
    assertEquals("error-section-tag-unknown", ex.errorCode());
  }

  private static SeekableByteChannel brokenChannel()
  {
    return new SeekableByteChannel()
    {
      @Override
      public int read(
        final ByteBuffer dst)
      {
        return 0;
      }

      @Override
      public int write(
        final ByteBuffer src)
      {
        return 0;
      }

      @Override
      public long position()
      {
        return 0;
      }

      @Override
      public SeekableByteChannel position(
        final long newPosition)
      {
        return null;
      }

      @Override
      public long size()
      {
        return 0;
      }

      @Override
      public SeekableByteChannel truncate(
        final long size)
      {
        return null;
      }

      @Override
      public boolean isOpen()
      {
        return false;
      }

      @Override
      public void close()
        throws IOException
      {
        throw new IOException("Wrong wire!");
      }
    };
  }
}
