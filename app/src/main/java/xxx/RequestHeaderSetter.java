package xxx;

import javax.annotation.Nullable;

import io.opentelemetry.context.propagation.TextMapSetter;
import okhttp3.Request;

enum RequestHeaderSetter implements TextMapSetter<Request.Builder> {
    INSTANCE;

    @Override
    public void set(@Nullable Request.Builder carrier, String key, String value) {
        if (carrier == null) {
            return;
        }
        carrier.header(key, value);
    }
}
