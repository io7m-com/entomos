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

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jbssio.api.BSSReaderProviderType;
import com.io7m.jbssio.api.BSSReaderRandomAccessType;
import com.io7m.jbssio.vanilla.BSSReaders;
import com.io7m.jmulticlose.core.CloseableCollection;
import com.io7m.jmulticlose.core.CloseableCollectionType;
import com.io7m.seltzer.io.SIOException;
import com.io7m.verona.core.Version;
import com.io7m.wendover.core.CloseShieldSeekableByteChannel;
import com.io7m.wendover.core.SubrangeSeekableByteChannel;

import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/**
 * A supplier of unchecked readers. These readers will not do
 * any file structure verification beyond the bare minimum required
 * to actually enumerate sections in the file.
 */

public final class EoFileReadersUnchecked
  implements EoFileReaderFactoryType<Void>
{
  private final BSSReaderProviderType readers;

  /**
   * A supplier of unchecked readers. These readers will not do
   * any file structure verification beyond the bare minimum required
   * to actually enumerate sections in the file.
   */

  public EoFileReadersUnchecked()
  {
    this(new BSSReaders());
  }

  /**
   * A supplier of unchecked readers. These readers will not do
   * any file structure verification beyond the bare minimum required
   * to actually enumerate sections in the file.
   *
   * @param inReaders A provider of {@code bssio} readers.
   */

  public EoFileReadersUnchecked(
    final BSSReaderProviderType inReaders)
  {
    this.readers =
      Objects.requireNonNull(inReaders, "readers");
  }

  @Override
  public EoFileReaderType forChannel(
    final URI uri,
    final long fileTag,
    final long endTag,
    final SeekableByteChannel channel,
    final Void parameters)
    throws EoException
  {
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(channel, "channel");

    Preconditions.checkPrecondition(
      fileTag != endTag,
      "File tag cannot equal end tag"
    );

    try {
      final var bssReader =
        this.readers.createReaderFromChannel(uri, channel, "File");
      final var fileReader =
        new EoFileReader(
          uri,
          fileTag,
          endTag,
          channel,
          bssReader
        );

      fileReader.start();
      return fileReader;
    } catch (final SIOException e) {
      throw EoException.wrap(e);
    }
  }

  private static final class EoFileReader
    implements EoFileReaderType
  {
    private final CloseableCollectionType<EoException> resources;
    private final URI uri;
    private final long fileTag;
    private final long endTag;
    private final SeekableByteChannel channel;
    private final BSSReaderRandomAccessType reader;
    private final TreeSet<EoFileSection> sections;
    private Version version;

    public EoFileReader(
      final URI inUri,
      final long inFileTag,
      final long inEndTag,
      final SeekableByteChannel inChannel,
      final BSSReaderRandomAccessType bssReader)
    {
      this.uri =
        Objects.requireNonNull(inUri, "uri");
      this.resources =
        CloseableCollection.create(() -> {
          return new EoException(
            "One or more resources could not be closed.",
            "error-resources",
            Map.of("File", inUri.toString()),
            Optional.empty()
          );
        });
      this.fileTag = inFileTag;
      this.endTag = inEndTag;

      this.sections =
        new TreeSet<>();
      this.channel =
        this.resources.add(inChannel);
      this.reader =
        this.resources.add(bssReader);
      this.version =
        Version.of(0, 0, 0);
    }

    @Override
    public NavigableSet<EoFileSection> sections()
    {
      return Collections.unmodifiableNavigableSet(this.sections);
    }

    @Override
    public long fileTag()
    {
      return this.fileTag;
    }

    @Override
    public Version version()
    {
      return this.version;
    }

    @Override
    public SeekableByteChannel dataChannel(
      final EoFileSection section)
      throws EoException
    {
      if (this.sections.contains(section)) {
        final var closeShield =
          new CloseShieldSeekableByteChannel(this.channel);
        return new SubrangeSeekableByteChannel(
          closeShield,
          section.dataOffset(),
          section.dataSize()
        );
      }

      throw this.errorNoSuchSection(section);
    }

    private EoException errorNoSuchSection(
      final EoFileSection section)
    {
      final var tag =
        "0x" + Long.toUnsignedString(section.tag(), 16);
      final var offset =
        "0x" + Long.toUnsignedString(section.offset(), 16);
      final var size =
        Long.toUnsignedString(section.dataSize());

      return new EoException(
        "No such file section.",
        "error-file-section-not-present",
        Map.ofEntries(
          Map.entry("File", this.uri.toString()),
          Map.entry("Tag", tag),
          Map.entry("Offset", offset),
          Map.entry("Size", size)
        ),
        Optional.empty()
      );
    }

    @Override
    public void close()
      throws EoException
    {
      this.resources.close();
    }

    public void start()
      throws EoException
    {
      try {
        this.readFileTag();
        this.readFileSections();
      } catch (final Throwable e) {
        this.close();
        throw EoException.wrap(e);
      }
    }

    private void readFileSections()
      throws SIOException, EoException
    {
      this.reader.seekTo(16L);

      while (this.enumerateFileSection()) {
        // Nothing
      }
    }

    private boolean enumerateFileSection()
      throws SIOException, EoException
    {
      final var offset =
        this.reader.offsetCurrentAbsolute();

      final long tag;
      try {
        tag = this.reader.readU64BE("SectionTag");
      } catch (final SIOException e) {
        throw this.errorFileEndMissing(e);
      }

      final var size =
        this.reader.readU64BE("SectionDataSize");

      this.reader.skip(size);
      seekTo16(this.reader);

      this.sections.add(
        EoFileSection.builder()
          .setDataSize(size)
          .setOffset(offset)
          .setTag(tag)
          .build()
      );
      return tag != this.endTag;
    }

    private EoException errorFileEndMissing(
      final SIOException e)
    {
      final var expected =
        "0x" + Long.toUnsignedString(this.endTag, 16);

      return new EoException(
        "File is missing an 'end' section.",
        e,
        "error-file-end-missing",
        Map.ofEntries(
          Map.entry("File", this.uri.toString()),
          Map.entry("Expected", expected),
          Map.entry("Offset", this.getOffset())
        ),
        Optional.empty()
      );
    }

    private String getOffset()
    {
      return "0x" + Long.toUnsignedString(
        this.reader.offsetCurrentAbsolute(),
        16);
    }

    private static void seekTo16(
      final BSSReaderRandomAccessType r)
      throws SIOException
    {
      final var position = r.offsetCurrentAbsolute();
      if (position % 16 == 0) {
        return;
      }

      final var next = position + (16L - (position % 16L));
      final var diff = next - position;
      r.skip(diff);
    }

    private void readFileTag()
      throws SIOException, EoException
    {
      this.reader.seekTo(0L);

      final long receivedFileTag;
      try {
        receivedFileTag = this.reader.readU64BE("FileTag");
      } catch (final SIOException e) {
        throw this.errorFileTagMissing(e);
      }

      if (receivedFileTag != this.fileTag) {
        throw this.errorFileTagIncorrect(receivedFileTag);
      }

      final long major;
      try {
        major = this.reader.readU32BE("VersionMajor");
      } catch (final SIOException e) {
        throw this.errorFileVersionMajorMissing(e);
      }

      final long minor;
      try {
        minor = this.reader.readU32BE("VersionMinor");
      } catch (final SIOException e) {
        throw this.errorFileVersionMinorMissing(e);
      }

      this.version =
        Version.of(
          (int) (major & 0xffffffffL),
          (int) (minor & 0xffffffffL),
          0
        );
    }

    private EoException errorFileVersionMinorMissing(
      final SIOException e)
    {
      final var expected =
        "0x" + Long.toUnsignedString(this.fileTag, 16);

      return new EoException(
        "Missing file minor version.",
        e,
        "error-file-version-minor-missing",
        Map.ofEntries(
          Map.entry("File", this.uri.toString()),
          Map.entry("Expected", expected),
          Map.entry("Offset", this.getOffset())
        ),
        Optional.empty()
      );
    }

    private EoException errorFileVersionMajorMissing(
      final SIOException e)
    {
      final var expected =
        "0x" + Long.toUnsignedString(this.fileTag, 16);

      return new EoException(
        "Missing file major version.",
        e,
        "error-file-version-major-missing",
        Map.ofEntries(
          Map.entry("File", this.uri.toString()),
          Map.entry("Expected", expected),
          Map.entry("Offset", this.getOffset())
        ),
        Optional.empty()
      );
    }

    private EoException errorFileTagMissing(
      final SIOException e)
    {
      final var expected =
        "0x" + Long.toUnsignedString(this.fileTag, 16);

      return new EoException(
        "Missing file tag.",
        e,
        "error-file-tag-missing",
        Map.ofEntries(
          Map.entry("File", this.uri.toString()),
          Map.entry("Expected", expected),
          Map.entry("Offset", this.getOffset())
        ),
        Optional.empty()
      );
    }

    private EoException errorFileTagIncorrect(
      final long receivedFileTag)
    {
      final var expected =
        "0x" + Long.toUnsignedString(this.fileTag, 16);
      final var received =
        "0x" + Long.toUnsignedString(receivedFileTag, 16);

      return new EoException(
        "Incorrect file tag.",
        "error-file-tag-incorrect",
        Map.ofEntries(
          Map.entry("File", this.uri.toString()),
          Map.entry("Expected", expected),
          Map.entry("Received", received)
        ),
        Optional.empty()
      );
    }
  }
}
