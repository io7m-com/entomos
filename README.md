entomos
===

[![Maven Central](https://img.shields.io/maven-central/v/com.io7m.entomos/com.io7m.entomos.svg?style=flat-square)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.io7m.entomos%22)
[![Maven Central (snapshot)](https://img.shields.io/nexus/s/com.io7m.entomos/com.io7m.entomos?server=https%3A%2F%2Fs01.oss.sonatype.org&style=flat-square)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/io7m/entomos/)
[![Codecov](https://img.shields.io/codecov/c/github/io7m-com/entomos.svg?style=flat-square)](https://codecov.io/gh/io7m-com/entomos)
![Java Version](https://img.shields.io/badge/21-java?label=java&color=e6c35c)

![com.io7m.entomos](./src/site/resources/entomos.jpg?raw=true)

| JVM | Platform | Status |
|-----|----------|--------|
| OpenJDK (Temurin) Current | Linux | [![Build (OpenJDK (Temurin) Current, Linux)](https://img.shields.io/github/actions/workflow/status/io7m-com/entomos/main.linux.temurin.current.yml)](https://www.github.com/io7m-com/entomos/actions?query=workflow%3Amain.linux.temurin.current)|
| OpenJDK (Temurin) LTS | Linux | [![Build (OpenJDK (Temurin) LTS, Linux)](https://img.shields.io/github/actions/workflow/status/io7m-com/entomos/main.linux.temurin.lts.yml)](https://www.github.com/io7m-com/entomos/actions?query=workflow%3Amain.linux.temurin.lts)|
| OpenJDK (Temurin) Current | Windows | [![Build (OpenJDK (Temurin) Current, Windows)](https://img.shields.io/github/actions/workflow/status/io7m-com/entomos/main.windows.temurin.current.yml)](https://www.github.com/io7m-com/entomos/actions?query=workflow%3Amain.windows.temurin.current)|
| OpenJDK (Temurin) LTS | Windows | [![Build (OpenJDK (Temurin) LTS, Windows)](https://img.shields.io/github/actions/workflow/status/io7m-com/entomos/main.windows.temurin.lts.yml)](https://www.github.com/io7m-com/entomos/actions?query=workflow%3Amain.windows.temurin.lts)|

## entomos

A utility library for implementing sectioned binary file formats.

### Features

  * Read and validate versioned, sectioned file formats.
  * Written in pure Java 21.
  * [OSGi](https://www.osgi.org/) ready.
  * [JPMS](https://en.wikipedia.org/wiki/Java_Platform_Module_System) ready.
  * ISC license.
  * High-coverage automated test suite.

### Motivation

Many [io7m](https://www.io7m.com) projects implement binary file formats.
With minor variations, all of the formats have tended to converge on the
following set of design rules:

  1. Files start with a 64-bit format-specific _tag_ for easy identification.
  2. The file tag is followed by a 32-bit _major_ and _minor_ version.
  3. The rest of the file is divided into a flat array of _sections_.
  4. Each _section_ is aligned to a 16-byte boundary.
  5. Each _section_ starts with a 64-bit format-specific _tag_.
  6. The section tag is followed by a 64-bit _size_ value, specifying the
     length of the data within the section.
  7. The section then contains _size_ bytes of data, followed by up to 15
     bytes of padding in order to ensure that any data that follows is
     aligned to a 16-byte boundary.
  8. The file ends with an _end section_. This is a normal
     _section_ where the section has a format-specific _end tag_, and a
     _size_ of `0`.
  9. Readers are required to stop reading at the _end section_. Trailing
     data is ignored.
  10. Readers are required to fail if there is no _end section_, and treat
      the file as corrupted and truncated.

All values are in _big-endian_ order. The data within sections can be in
any byte order as required by specific formats.

Some formats have rules on _ordering_: A section with a particular tag might
be required to be the first one in the file, or the last one in the file
(just prior to the _end_ section). No format to date has apparently required
a very strict order on sections, and this would appear to be of limited
utility.

Some formats have rules on _cardinality_: A section with a particular tag
might be required to appear exactly once, at least once, at most once, or
any number of times.

For all formats, [semantic versioning](https://www.semver.org) has tended to be
used. The formats tend to come with versioning rules akin to the following:

1. The specification is versioned via a restricted subset of the Semantic
   Versioning specification. The specification has a major and minor version
   number, with major version increments denoting incompatible changes, and
   minor version increments denoting new functionality. There is no patch
   version number. A version of the specification with major version `m` and
   minor version `n` is denoted as specification version `(m, n)`.

2. Assuming a version of the specification `m`, an update to the specification
   that yields version `n` such that `n > m` is considered to be
   _forwards compatible_ if a parser that supports format version `m` can read
   files that were written using format version `n`.

3. Assuming a version of the specification `m`, an update to the specification
   that yields version `n` such that `n > m` is considered to be
   _backwards compatible_ if a parser that supports format version `n` can read
   files that were written using format version `m`.

4. The specification is designed such that a correctly-written parser
   implementation that supports a major version `m` is able to support the set
   of versions `∀n. (m, n)`. This implies full forwards and backwards
   compatibility for parsers when the major version is unchanged.

5. Changes that would cause a parser supporting an older version of the
   specification to fail to read a file written according to a newer version
   of the specification MUST imply an increment in the major version of the
   specification.

6. Changes that would cause a parser supporting a newer version of the
   specification to fail to read a file written according to an older
   version of the specification MUST imply an increment in the major version
   of the specification.

An implication of the above rules is that new features added to the
format specification must be added in a manner that allows them to be ignored
by older parsers, lest the major version of the specification be incremented
on every update.

```
00000000  89 43 4c 4e 0d 0a 1a 0a  00 00 00 01 00 00 00 00  |.CLN............|
00000010  43 4c 4e 49 49 4e 46 4f  00 00 00 00 00 00 00 a0  |CLNIINFO........|
00000020  00 00 01 00 00 00 01 00  00 00 00 06 00 00 00 0b  |................|
00000030  52 38 3a 47 38 3a 42 38  3a 41 38 00 00 00 00 1f  |R8:G8:B8:A8.....|
00000040  46 49 58 45 44 5f 50 4f  49 4e 54 5f 4e 4f 52 4d  |FIXED_POINT_NORM|
00000050  41 4c 49 5a 45 44 5f 55  4e 53 49 47 4e 45 44 00  |ALIZED_UNSIGNED.|
00000060  00 00 00 0c 55 4e 43 4f  4d 50 52 45 53 53 45 44  |....UNCOMPRESSED|
00000070  00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00  |................|
00000080  00 00 00 00 00 00 00 03  4c 5a 34 00 00 00 00 00  |........LZ4.....|
00000090  00 00 00 00 00 00 00 08  52 54 3a 53 52 3a 54 44  |........RT:SR:TD|
000000a0  00 00 00 04 53 52 47 42  00 00 00 00 00 00 00 0d  |....SRGB........|
000000b0  4c 49 54 54 4c 45 5f 45  4e 44 49 41 4e 00 00 00  |LITTLE_ENDIAN...|
000000c0  43 4c 4e 5f 41 52 52 21  00 00 00 00 00 12 30 e0  |CLN_ARR!......0.|
000000d0  00 00 00 30 00 00 00 07  00 00 00 00 00 00 00 00  |...0............|
000000e0  00 00 06 e0 00 00 00 00  00 00 00 10 00 00 00 00  |................|
```

The general structure of the file formats could be seen as similar to a
flattened and simplified form of the [RIFF](https://en.wikipedia.org/wiki/Resource_Interchange_File_Format)
format, but using 64-bit instead of 32-bit values.

The `entomos` package provides a set of primitives for implementing file
formats that adhere to the general design rules above. It is intended to
reduce the amount of effectively duplicated code between projects, providing
a simple API for parsing, validating, and extracting data from files.

No code is provided for _writing_ files, because that code is already trivial
and is typically implemented by just using
[jbssio](https://www.io7m.com/software/jbssio/) directly; there don't appear
to be any abstractions that would reduce code duplication and/or make the
code any mechanically simpler than it already is.

### Building

```
$ mvn clean verify
```

### Usage

Declare a _file format_ by declaring the _sections_. The
`EoTags` class provides some helpful methods for picking 64-bit values that
can have various useful properties. The various builder classes check various
required invariants such as tags being unique, no conflicting ordering
declarations, and etc.

```
/// 0x8958595A0D0A1A0A
final long fileTag =
  EoTags.pngStyle((byte) 'X', (byte), (byte) 'Y', (byte) 'Z');

// 0x4558414D_54414741
final long tagA =
  EoTags.ofString("EXAMTAGA");

// 0x4558414D_54414742
final long tagB =
  EoTags.ofString("EXAMTAGB");

// 0x4558414D_54414742
final long tagC =
  EoTags.ofString("EXAMTAGC");

// 0x4558414D_454E4421
final long endTag =
  EoTags.ofString("EXAMEND!");

// A section with tag A, that must appear exactly once, and must be first.
final var sectionA =
  EoFileSectionDescription.builder()
    .setTag(tagA)
    .setCardinality(ONE)
    .setOrdering(MUST_BE_FIRST)
    .build();

// A section with tag B, that must appear at least once.
final var sectionB =
  EoFileSectionDescription.builder()
    .setTag(tagA)
    .setCardinality(ONE_TO_N)
    .build();

// A section with tag C, that must appear at most once, and must be last.
final var sectionC =
  EoFileSectionDescription.builder()
    .setTag(tagC)
    .setCardinality(ZERO_TO_ONE)
    .setOrdering(MUST_BE_LAST)
    .build();

// Format version 1.0
final var format1 =
  EoFileDescription.builder()
    .setVersionMajor(1)
    .setVersionMinor(0)
    .setFileTag(fileTag)
    .setEndTag(endTag)
    .addSections(sectionA, sectionB, sectionC)
    .build();

// All versions of the format.
final var formatAll =
  EoFileVersionsDescription.builder()
    .addDescriptions(format1)
    .build();
```

Open a _reader_ that can validate and read files:

```
final var readers =
  new EoFileReadersChecked();

final Path path = ...;

try (final var reader = readers.forFile(tagFile, tagEnd, path, formatAll)) {
  System.out.printf("Version: %s%n", reader.version());
  System.out.printf("Sections: %s%n", reader.sections());

  try (SeekableByteChannel channel = reader.dataChannel(reader.sections.first())) {
    // `channel` is bounded and can only read from within the data of the
    // passed-in section.
  }
}
```

The _checked_ reader validates that the file sections obey the declared
cardinality and ordering rules, and that the file is well-formed in other
ways (such as not being truncated). It also checks that the file tag matches
that of the declared format, and that the major version of the file is equal
to one of the declared versions. The `entomos` implementation performs the
minimum amount of I/O needed to validate files, and critically does not
read the entire file into memory (although it will perform one seek for
each section in the file in order to read section tags and sizes). No data
within sections is actually read until `reader.dataChannel()` is used.

All interfaces produce detailed
[structured exceptions](https://github.com/io7m-com/seltzer) in the case
of errors.

An _unchecked_ reader is also provided that merely enumerates sections within
the file. The _checked_ reader is implemented on top of the _unchecked_ reader.

