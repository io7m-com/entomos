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

import com.io7m.jbssio.api.BSSReaderProviderType;
import com.io7m.jbssio.vanilla.BSSReaders;
import com.io7m.verona.core.Version;

import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A supplier of checked readers. These readers will validate that the file
 * confirms to a given specification.
 */

public final class EoFileReadersChecked
  implements EoFileReaderFactoryType<EoFileVersionsDescription>
{
  private final EoFileReadersUnchecked unchecked;

  /**
   * A supplier of checked readers. These readers will validate that the file
   * confirms to a given specification.
   */

  public EoFileReadersChecked()
  {
    this(new BSSReaders());
  }

  /**
   * A supplier of checked readers. These readers will validate that the file
   * confirms to a given specification.
   *
   * @param inReaders A provider of {@code bssio} readers.
   */

  public EoFileReadersChecked(
    final BSSReaderProviderType inReaders)
  {
    this.unchecked =
      new EoFileReadersUnchecked(inReaders);
  }

  @Override
  public EoFileReaderType forChannel(
    final URI uri,
    final long fileTag,
    final long endTag,
    final SeekableByteChannel channel,
    final EoFileVersionsDescription parameters)
    throws EoException
  {
    Objects.requireNonNull(uri, "uri");
    Objects.requireNonNull(channel, "channel");
    Objects.requireNonNull(parameters, "parameters");

    final var checked =
      new EoFileReaderChecked(
        uri,
        this.unchecked.forChannel(uri, fileTag, endTag, channel, null),
        parameters
      );

    checked.start();
    return checked;
  }

  private static final class EoFileReaderChecked
    implements EoFileReaderType
  {
    private final URI uri;
    private final EoFileReaderType baseReader;
    private final EoFileVersionsDescription versions;

    public EoFileReaderChecked(
      final URI inUri,
      final EoFileReaderType inBaseReader,
      final EoFileVersionsDescription inParameters)
    {
      this.uri =
        Objects.requireNonNull(inUri, "uri");
      this.baseReader =
        Objects.requireNonNull(inBaseReader, "baseReader");
      this.versions =
        Objects.requireNonNull(inParameters, "parameters");
    }

    @Override
    public NavigableSet<EoFileSection> sections()
    {
      return this.baseReader.sections();
    }

    @Override
    public long fileTag()
    {
      return this.baseReader.fileTag();
    }

    @Override
    public Version version()
    {
      return this.baseReader.version();
    }

    @Override
    public SeekableByteChannel dataChannel(
      final EoFileSection section)
      throws EoException
    {
      return this.baseReader.dataChannel(section);
    }

    @Override
    public void close()
      throws EoException
    {
      this.baseReader.close();
    }

    public void start()
      throws EoException
    {
      try {
        final var receivedVersion =
          this.version();
        final var format =
          this.findBestFormat(receivedVersion);
        if (format.isEmpty()) {
          throw this.errorVersionNotSupported(receivedVersion);
        }

        this.checkFormat(format.get());
      } catch (final Throwable e) {
        this.close();
        throw EoException.wrap(e);
      }
    }

    private void checkFormat(
      final EoFileDescription description)
      throws EoException
    {
      this.checkSectionsKnown(description);
      this.checkSectionOrdering(description);
      this.checkSectionCardinality(description);
    }

    private void checkSectionCardinality(
      final EoFileDescription description)
      throws EoException
    {
      final var tagCounts = new HashMap<Long, Long>();
      for (final var fileSection : this.sections()) {
        final var count =
          tagCounts.getOrDefault(fileSection.tag(), 0L) + 1;
        tagCounts.put(fileSection.tag(), count);
      }

      for (final var section : description.sections()) {
        final var count =
          tagCounts.getOrDefault(section.tag(), 0L);

        switch (section.cardinality()) {
          case ONE -> {
            if (count != 1L) {
              throw this.errorSectionCardinality(section, count);
            }
          }
          case ZERO_TO_ONE -> {
            if (count > 1L) {
              throw this.errorSectionCardinality(section, count);
            }
          }
          case ZERO_TO_N -> {
            // Nothing to do
          }
          case ONE_TO_N -> {
            if (count < 1L) {
              throw this.errorSectionCardinality(section, count);
            }
          }
        }
      }
    }

    private EoException errorSectionCardinality(
      final EoFileSectionDescription section,
      final Long count)
    {
      final var sectionTagText =
        "0x" + Long.toUnsignedString(section.tag(), 16);

      return new EoException(
        "Section cardinality violation; too many or too few sections with this tag.",
        "error-section-tag-cardinality",
        Map.ofEntries(
          Map.entry("File", this.uri.toString()),
          Map.entry("Tag", sectionTagText),
          Map.entry("Cardinality", section.cardinality().toString()),
          Map.entry("Section Count", count.toString())
        ),
        Optional.empty()
      );
    }

    private void checkSectionOrdering(
      final EoFileDescription description)
      throws EoException
    {
      for (final var section : description.sections()) {
        switch (section.ordering()) {
          case MUST_BE_FIRST -> {
            final var first =
              this.sections().first();
            if (first.tag() != section.tag()) {
              throw this.errorSectionNotFirst(section, first);
            }
          }
          case MUST_BE_LAST -> {
            final var lastNotEnd =
              this.sections().lower(this.sections().last());
            if (lastNotEnd.tag() != section.tag()) {
              throw this.errorSectionNotLast(section, lastNotEnd);
            }
          }
          case ANY_ORDER -> {
            // Do nothing.
          }
        }
      }
    }

    private EoException errorSectionNotLast(
      final EoFileSectionDescription section,
      final EoFileSection last)
    {
      final var sectionTagText =
        "0x" + Long.toUnsignedString(section.tag(), 16);
      final var firstTagText =
        "0x" + Long.toUnsignedString(last.tag(), 16);
      final var offset =
        "0x" + Long.toUnsignedString(last.offset(), 16);

      return new EoException(
        "The last section is not of the required tag.",
        "error-section-tag-last",
        Map.ofEntries(
          Map.entry("File", this.uri.toString()),
          Map.entry("Offset", offset),
          Map.entry("Required Tag", sectionTagText),
          Map.entry("Received Tag", firstTagText)
        ),
        Optional.empty()
      );
    }

    private EoException errorSectionNotFirst(
      final EoFileSectionDescription section,
      final EoFileSection first)
    {
      final var sectionTagText =
        "0x" + Long.toUnsignedString(section.tag(), 16);
      final var firstTagText =
        "0x" + Long.toUnsignedString(first.tag(), 16);
      final var offset =
        "0x" + Long.toUnsignedString(first.offset(), 16);

      return new EoException(
        "The first section is not of the required tag.",
        "error-section-tag-first",
        Map.ofEntries(
          Map.entry("File", this.uri.toString()),
          Map.entry("Offset", offset),
          Map.entry("Required Tag", sectionTagText),
          Map.entry("Received Tag", firstTagText)
        ),
        Optional.empty()
      );
    }

    private void checkSectionsKnown(
      final EoFileDescription description)
      throws EoException
    {
      switch (description.sectionsUnknown()) {
        case UNKNOWN_SECTIONS_PERMITTED -> {
          // Do nothing.
        }
        case UNKNOWN_SECTIONS_NOT_PERMITTED -> {
          for (final var section : this.sections()) {
            if (section.tag() == description.endTag()) {
              continue;
            }
            if (!description.sectionByTag().containsKey(section.tag())) {
              throw this.errorSectionUnknown(description, section);
            }
          }
        }
      }
    }

    private EoException errorSectionUnknown(
      final EoFileDescription description,
      final EoFileSection section)
    {
      final var supported =
        description.sections()
          .stream()
          .map(EoFileSectionDescription::tag)
          .map(t -> "0x" + Long.toUnsignedString(t, 16))
          .collect(Collectors.joining(", "));

      final var tagText =
        "0x" + Long.toUnsignedString(section.tag(), 16);
      final var offset =
        "0x" + Long.toUnsignedString(section.offset(), 16);

      return new EoException(
        "A section with an unknown tag was encountered, and unknown tags are not permitted.",
        "error-section-tag-unknown",
        Map.ofEntries(
          Map.entry("File", this.uri.toString()),
          Map.entry("Offset", offset),
          Map.entry("Tag", tagText),
          Map.entry("Supported", supported)
        ),
        Optional.empty()
      );
    }

    private Optional<EoFileDescription> findBestFormat(
      final Version version)
    {
      final var versionMap = this.versions.versions();
      return versionMap.keySet()
        .stream()
        .filter(v -> v.major() == version.major())
        .max(Version::compareTo)
        .flatMap(v -> Optional.ofNullable(versionMap.get(v)));
    }

    private EoException errorVersionNotSupported(
      final Version receivedVersion)
    {
      final var supported =
        this.versions.versions()
          .keySet()
          .stream()
          .map(Version::toString)
          .collect(Collectors.joining(", "));

      return new EoException(
        "File format version is not supported.",
        "error-file-version-not-supported",
        Map.ofEntries(
          Map.entry("File", this.uri.toString()),
          Map.entry("Version", receivedVersion.toString()),
          Map.entry("Supported", supported)
        ),
        Optional.empty()
      );
    }
  }
}
