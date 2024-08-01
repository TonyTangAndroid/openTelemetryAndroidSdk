
package xxx;

import static java.util.Collections.singleton;
import static xxx.SpanSuppressors.BySpanKey;
import static xxx.SpanSuppressors.DelegateBySpanKind;
import static xxx.SpanSuppressors.Noop;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.api.internal.SpanKey;

enum SpanSuppressionStrategy {
    NONE {
        @Override
        SpanSuppressor create(Set<SpanKey> spanKeys) {
            return Noop.INSTANCE;
        }
    },
    SPAN_KIND {
        private final SpanSuppressor strategy;

        {
            Map<SpanKind, SpanSuppressor> delegates = new EnumMap<>(SpanKind.class);
            delegates.put(SpanKind.SERVER, new BySpanKey(singleton(SpanKey.KIND_SERVER)));
            delegates.put(SpanKind.CLIENT, new BySpanKey(singleton(SpanKey.KIND_CLIENT)));
            delegates.put(SpanKind.CONSUMER, new BySpanKey(singleton(SpanKey.KIND_CONSUMER)));
            delegates.put(SpanKind.PRODUCER, new BySpanKey(singleton(SpanKey.KIND_PRODUCER)));
            strategy = new DelegateBySpanKind(delegates);
        }

        @Override
        SpanSuppressor create(Set<SpanKey> spanKeys) {
            return strategy;
        }
    },

    SEMCONV {
        @Override
        SpanSuppressor create(Set<SpanKey> spanKeys) {
            if (spanKeys.isEmpty()) {
                return Noop.INSTANCE;
            }
            return new BySpanKey(spanKeys);
        }
    };

    abstract SpanSuppressor create(Set<SpanKey> spanKeys);

    static SpanSuppressionStrategy fromConfig(@Nullable String value) {
        if (value == null) {
            value = "semconv";
        }
        switch (value.toLowerCase(Locale.ROOT)) {
            case "none":
                return NONE;
            case "span-kind":
                return SPAN_KIND;
            default:
                return SEMCONV;
        }
    }
}
