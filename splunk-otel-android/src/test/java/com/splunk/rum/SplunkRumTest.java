/*
 * Copyright Splunk Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.splunk.rum;

import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Application;
import android.content.Context;
import android.location.Location;
import android.webkit.WebView;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.testing.junit4.OpenTelemetryRule;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class SplunkRumTest {

    @Rule public OpenTelemetryRule otelTesting = OpenTelemetryRule.create();

    private Tracer tracer;
    private final AtomicReference<Attributes> globalAttributes =
            new AtomicReference<>(Attributes.of(stringKey("key"), "value"));

    @Before
    public void setup() {
        tracer = otelTesting.getOpenTelemetry().getTracer("testTracer");
        SplunkRum.resetSingletonForTest();
    }

    @Test
    public void initialization_onlyOnce() {
        Application application = mock(Application.class, RETURNS_DEEP_STUBS);
        ConnectionUtil.Factory connectionUtilFactory =
                mock(ConnectionUtil.Factory.class, RETURNS_DEEP_STUBS);
        Context context = mock(Context.class);

        SplunkRumBuilder splunkRumBuilder =
                new SplunkRumBuilder()
                        .setApplicationName("appName")
                        .setBeaconEndpoint("http://backend")
                        .setRumAccessToken("abracadabra")
                        .disableAnrDetection();

        when(application.getApplicationContext()).thenReturn(context);
        when(context.getFilesDir()).thenReturn(new File("/my/storage/spot"));

        SplunkRum singleton =
                SplunkRum.initialize(splunkRumBuilder, application, connectionUtilFactory);
        SplunkRum sameInstance = splunkRumBuilder.build(application);

        assertSame(singleton, sameInstance);
    }

    @Test
    public void getInstance_preConfig() {
        SplunkRum instance = SplunkRum.getInstance();
        assertTrue(instance instanceof NoOpSplunkRum);
    }

    @Test
    public void getInstance() {
        Application application = mock(Application.class, RETURNS_DEEP_STUBS);
        ConnectionUtil.Factory connectionUtilFactory =
                mock(ConnectionUtil.Factory.class, RETURNS_DEEP_STUBS);
        Context context = mock(Context.class);

        SplunkRumBuilder splunkRumBuilder =
                new SplunkRumBuilder()
                        .setApplicationName("appName")
                        .setBeaconEndpoint("http://backend")
                        .setRumAccessToken("abracadabra")
                        .disableAnrDetection();

        when(application.getApplicationContext()).thenReturn(context);
        when(context.getFilesDir()).thenReturn(new File("/my/storage/spot"));

        SplunkRum singleton =
                SplunkRum.initialize(splunkRumBuilder, application, connectionUtilFactory);
        assertSame(singleton, SplunkRum.getInstance());
    }

    @Test
    public void newBuilder() {
        assertNotNull(SplunkRum.builder());
    }

    @Test
    public void nonNullMethods() {
        Application application = mock(Application.class, RETURNS_DEEP_STUBS);
        ConnectionUtil.Factory connectionUtilFactory =
                mock(ConnectionUtil.Factory.class, RETURNS_DEEP_STUBS);
        Context context = mock(Context.class);

        when(application.getApplicationContext()).thenReturn(context);
        when(context.getFilesDir()).thenReturn(new File("/my/storage/spot"));

        SplunkRumBuilder splunkRumBuilder =
                new SplunkRumBuilder()
                        .setApplicationName("appName")
                        .setBeaconEndpoint("http://backend")
                        .setRumAccessToken("abracadabra")
                        .disableAnrDetection();

        SplunkRum splunkRum =
                SplunkRum.initialize(splunkRumBuilder, application, connectionUtilFactory);
        assertNotNull(splunkRum.getOpenTelemetry());
        assertNotNull(splunkRum.getRumSessionId());
    }

    @Test
    public void addEvent() {
        SplunkRum splunkRum =
                new SplunkRum(
                        (OpenTelemetrySdk) otelTesting.getOpenTelemetry(),
                        new SessionId(new SessionIdTimeoutHandler()),
                        globalAttributes);

        Attributes attributes = Attributes.of(stringKey("one"), "1", longKey("two"), 2L);
        splunkRum.addRumEvent("foo", attributes);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        assertEquals("foo", spans.get(0).getName());
        assertEquals(attributes.asMap(), spans.get(0).getAttributes().asMap());
    }

    @Test
    public void updateGlobalAttributes() {
        SplunkRum splunkRum =
                new SplunkRum(
                        (OpenTelemetrySdk) otelTesting.getOpenTelemetry(),
                        new SessionId(new SessionIdTimeoutHandler()),
                        globalAttributes);

        splunkRum.updateGlobalAttributes(
                attributesBuilder -> attributesBuilder.put("key", "value2"));
        splunkRum.setGlobalAttribute(longKey("otherKey"), 1234L);

        assertEquals(
                Attributes.of(stringKey("key"), "value2", longKey("otherKey"), 1234L),
                globalAttributes.get());
    }

    @Test
    public void recordAnr() {
        StackTraceElement[] stackTrace = new Exception().getStackTrace();
        StringBuilder stringBuilder = new StringBuilder();
        for (StackTraceElement stackTraceElement : stackTrace) {
            stringBuilder.append(stackTraceElement).append("\n");
        }

        SplunkRum splunkRum =
                new SplunkRum(
                        (OpenTelemetrySdk) otelTesting.getOpenTelemetry(),
                        new SessionId(new SessionIdTimeoutHandler()),
                        globalAttributes);

        Attributes expectedAttributes =
                Attributes.of(
                        SemanticAttributes.EXCEPTION_STACKTRACE,
                        stringBuilder.toString(),
                        SplunkRum.COMPONENT_KEY,
                        SplunkRum.COMPONENT_ERROR);

        splunkRum.recordAnr(stackTrace);

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(1, spans.size());
        SpanData anrSpan = spans.get(0);
        assertEquals("ANR", anrSpan.getName());
        assertEquals(expectedAttributes.asMap(), anrSpan.getAttributes().asMap());
        assertEquals(StatusData.error(), anrSpan.getStatus());
    }

    @Test
    public void addException() {
        InMemorySpanExporter testExporter = InMemorySpanExporter.create();
        OpenTelemetrySdk testSdk = buildTestSdk(testExporter);

        SplunkRum splunkRum =
                new SplunkRum(
                        testSdk, new SessionId(new SessionIdTimeoutHandler()), globalAttributes);

        NullPointerException exception = new NullPointerException("oopsie");
        Attributes attributes = Attributes.of(stringKey("one"), "1", longKey("two"), 2L);
        splunkRum.addRumException(exception, attributes);

        List<SpanData> spans = testExporter.getFinishedSpanItems();
        assertEquals(1, spans.size());

        assertThat(spans.get(0))
                .hasName("NullPointerException")
                .hasAttributes(
                        attributes.toBuilder()
                                .put(SplunkRum.COMPONENT_KEY, SplunkRum.COMPONENT_ERROR)
                                .build())
                .hasException(exception);
    }

    private OpenTelemetrySdk buildTestSdk(InMemorySpanExporter testExporter) {
        return OpenTelemetrySdk.builder()
                .setTracerProvider(
                        SdkTracerProvider.builder()
                                .addSpanProcessor(SimpleSpanProcessor.create(testExporter))
                                .build())
                .build();
    }

    @Test
    public void createAndEnd() {
        SplunkRum splunkRum =
                new SplunkRum(
                        (OpenTelemetrySdk) otelTesting.getOpenTelemetry(),
                        new SessionId(new SessionIdTimeoutHandler()),
                        globalAttributes);

        Span span = splunkRum.startWorkflow("workflow");
        Span inner = tracer.spanBuilder("foo").startSpan();
        try (Scope scope = inner.makeCurrent()) {
            // do nothing
        } finally {
            inner.end();
        }
        span.end();

        List<SpanData> spans = otelTesting.getSpans();
        assertEquals(2, spans.size());
        // verify we're not trying to do any propagation of the context here.
        assertEquals(spans.get(0).getParentSpanId(), SpanId.getInvalid());
        assertEquals("foo", spans.get(0).getName());
        assertEquals("workflow", spans.get(1).getName());
        assertEquals("workflow", spans.get(1).getAttributes().get(SplunkRum.WORKFLOW_NAME_KEY));
    }

    @Test
    public void integrateWithBrowserRum() {
        Application application = mock(Application.class, RETURNS_DEEP_STUBS);
        ConnectionUtil.Factory connectionUtilFactory =
                mock(ConnectionUtil.Factory.class, RETURNS_DEEP_STUBS);
        Context context = mock(Context.class);
        WebView webView = mock(WebView.class);

        when(application.getApplicationContext()).thenReturn(context);
        when(context.getFilesDir()).thenReturn(new File("/my/storage/spot"));

        SplunkRumBuilder splunkRumBuilder =
                new SplunkRumBuilder()
                        .setApplicationName("appName")
                        .setBeaconEndpoint("http://backend")
                        .setRumAccessToken("abracadabra")
                        .disableAnrDetection();

        SplunkRum splunkRum =
                SplunkRum.initialize(splunkRumBuilder, application, connectionUtilFactory);
        splunkRum.integrateWithBrowserRum(webView);

        verify(webView)
                .addJavascriptInterface(isA(NativeRumSessionId.class), eq("SplunkRumNative"));
    }

    @Test
    public void updateLocation() {
        AtomicReference<Attributes> globalAttributes = new AtomicReference<>(Attributes.empty());
        SplunkRum splunkRum =
                new SplunkRum(
                        (OpenTelemetrySdk) otelTesting.getOpenTelemetry(),
                        new SessionId(new SessionIdTimeoutHandler()),
                        globalAttributes);

        Location location = mock(Location.class);
        when(location.getLatitude()).thenReturn(42d);
        when(location.getLongitude()).thenReturn(43d);
        splunkRum.updateLocation(location);

        assertEquals(
                Attributes.of(
                        SplunkRum.LOCATION_LATITUDE_KEY,
                        42d,
                        SplunkRum.LOCATION_LONGITUDE_KEY,
                        43d),
                globalAttributes.get());

        splunkRum.updateLocation(null);

        assertTrue(globalAttributes.get().isEmpty());
    }
}
