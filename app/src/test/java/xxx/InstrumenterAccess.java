/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package xxx;

import java.time.Instant;

import javax.annotation.Nullable;

import io.opentelemetry.context.Context;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public interface InstrumenterAccess {

  <REQUEST, RESPONSE> Context startAndEnd(
      Instrumenter<REQUEST, RESPONSE> instrumenter,
      Context parentContext,
      REQUEST request,
      @Nullable RESPONSE response,
      @Nullable Throwable error,
      Instant startTime,
      Instant endTime);

  <REQUEST, RESPONSE> Context suppressSpan(
      Instrumenter<REQUEST, RESPONSE> instrumenter, Context parentContext, REQUEST request);
}
