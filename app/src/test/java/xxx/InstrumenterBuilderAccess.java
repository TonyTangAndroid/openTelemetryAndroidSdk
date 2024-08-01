package xxx;

import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public interface InstrumenterBuilderAccess {

    <REQUEST, RESPONSE> Instrumenter<REQUEST, RESPONSE> buildUpstreamInstrumenter(
            InstrumenterBuilder<REQUEST, RESPONSE> builder,
            TextMapGetter<REQUEST> getter,
            SpanKindExtractor<REQUEST> spanKindExtractor);

    <REQUEST, RESPONSE> Instrumenter<REQUEST, RESPONSE> buildDownstreamInstrumenter(
            InstrumenterBuilder<REQUEST, RESPONSE> builder,
            TextMapSetter<REQUEST> setter,
            SpanKindExtractor<REQUEST> spanKindExtractor);
}
