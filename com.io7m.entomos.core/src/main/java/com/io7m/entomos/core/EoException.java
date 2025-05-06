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

import com.io7m.seltzer.api.SStructuredErrorExceptionType;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/**
 * The type of exceptions.
 */

public final class EoException
  extends Exception
  implements SStructuredErrorExceptionType<String>
{
  private final String errorCode;
  private final Map<String, String> attributes;
  private final Optional<String> remediatingAction;

  /**
   * Construct an exception.
   *
   * @param message             The message
   * @param inErrorCode         The error code
   * @param inAttributes        The error attributes
   * @param inRemediatingAction The remediating action, if any
   */

  public EoException(
    final String message,
    final String inErrorCode,
    final Map<String, String> inAttributes,
    final Optional<String> inRemediatingAction)
  {
    super(Objects.requireNonNull(message, "message"));

    this.errorCode =
      Objects.requireNonNull(inErrorCode, "errorCode");
    this.attributes =
      Objects.requireNonNull(inAttributes, "attributes");
    this.remediatingAction =
      Objects.requireNonNull(inRemediatingAction, "remediatingAction");
  }

  /**
   * Construct an exception.
   *
   * @param message             The message
   * @param cause               The cause
   * @param inErrorCode         The error code
   * @param inAttributes        The error attributes
   * @param inRemediatingAction The remediating action, if any
   */

  public EoException(
    final String message,
    final Throwable cause,
    final String inErrorCode,
    final Map<String, String> inAttributes,
    final Optional<String> inRemediatingAction)
  {
    super(
      Objects.requireNonNull(message, "message"),
      Objects.requireNonNull(cause, "cause")
    );
    this.errorCode =
      Objects.requireNonNull(inErrorCode, "errorCode");
    this.attributes =
      Objects.requireNonNull(inAttributes, "attributes");
    this.remediatingAction =
      Objects.requireNonNull(inRemediatingAction, "remediatingAction");
  }

  @Override
  public String errorCode()
  {
    return this.errorCode;
  }

  @Override
  public Map<String, String> attributes()
  {
    return this.attributes;
  }

  @Override
  public Optional<String> remediatingAction()
  {
    return this.remediatingAction;
  }

  @Override
  public Optional<Throwable> exception()
  {
    return Optional.of(this);
  }

  /**
   * Wrap an existing exception with the minimum required to make it an
   * {@code entomos} exception.
   *
   * @param exception The exception
   *
   * @return A wrapper exception
   */

  public static EoException wrap(
    final Throwable exception)
  {
    return switch (exception) {
      case final EoException e -> e;

      case final SStructuredErrorExceptionType<?> x -> {
        yield new EoException(
          x.message(),
          exception,
          x.errorCode().toString(),
          x.attributes(),
          x.remediatingAction()
        );
      }

      case final ExecutionException e -> {
        final var cause = e.getCause();
        if (cause != null) {
          yield wrap(cause);
        }
        yield new EoException(
          Objects.requireNonNullElse(
            exception.getMessage(),
            "An unexpected exception occurred."
          ),
          exception,
          "error-exception",
          Map.of(),
          Optional.empty()
        );
      }

      default -> {
        yield new EoException(
          Objects.requireNonNullElse(
            exception.getMessage(),
            "An unexpected exception occurred."
          ),
          exception,
          "error-exception",
          Map.of(),
          Optional.empty()
        );
      }
    };
  }
}
