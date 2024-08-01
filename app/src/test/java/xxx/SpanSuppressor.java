package xxx;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;

interface SpanSuppressor {

    Context storeInContext(Context context, SpanKind spanKind, Span span);

    boolean shouldSuppress(Context parentContext, SpanKind spanKind);
}
