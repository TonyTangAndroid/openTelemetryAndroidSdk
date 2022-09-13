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

import static com.splunk.rum.DeviceSpanStorageLimiter.DEFAULT_MAX_STORAGE_USE_MB;

import android.util.Log;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import java.time.Duration;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Configuration class for the Splunk Android RUM (Real User Monitoring) library.
 *
 * <p>Both the beaconUrl and the rumAuthToken are mandatory configuration settings. Trying to build
 * a Config instance without both of these items specified will result in an exception being thrown.
 *
 * @deprecated Use {@link #builder()} and the {@link SplunkRumBuilder} to configure a {@link
 *     SplunkRum} instance.
 */
@Deprecated
public class Config {

    private final String beaconEndpoint;
    private final String rumAccessToken;
    private final boolean debugEnabled;
    private final String applicationName;
    private final boolean crashReportingEnabled;
    private final boolean networkMonitorEnabled;
    private final boolean anrDetectionEnabled;
    private final Attributes globalAttributes;
    private final Function<SpanExporter, SpanExporter> spanFilterExporterDecorator;
    private final Consumer<SpanFilterBuilder> spanFilterBuilderConfigurer;
    private final boolean slowRenderingDetectionEnabled;
    private final Duration slowRenderingDetectionPollInterval;
    private final boolean diskBufferingEnabled;
    private final int maxUsageMegabytes;
    private final boolean sessionBasedSamplerEnabled;
    private final double sessionBasedSamplerRatio;

    private Config(Builder builder) {
        this.beaconEndpoint = builder.beaconEndpoint;
        this.rumAccessToken = builder.rumAccessToken;
        this.debugEnabled = builder.debugEnabled;
        this.applicationName = builder.applicationName;
        this.crashReportingEnabled = builder.crashReportingEnabled;
        this.globalAttributes = addDeploymentEnvironment(builder);
        this.networkMonitorEnabled = builder.networkMonitorEnabled;
        this.anrDetectionEnabled = builder.anrDetectionEnabled;
        this.slowRenderingDetectionPollInterval = builder.slowRenderingDetectionPollInterval;
        this.slowRenderingDetectionEnabled = builder.slowRenderingDetectionEnabled;
        this.spanFilterExporterDecorator = builder.spanFilterBuilder.build();
        this.spanFilterBuilderConfigurer = builder.spanFilterBuilderConfigurer;
        this.diskBufferingEnabled = builder.diskBufferingEnabled;
        this.maxUsageMegabytes = builder.maxUsageMegabytes;
        this.sessionBasedSamplerEnabled = builder.sessionBasedSamplerEnabled;
        this.sessionBasedSamplerRatio = builder.sessionBasedSamplerRatio;
    }

    private Attributes addDeploymentEnvironment(Builder builder) {
        Attributes globalAttributes = builder.globalAttributes;
        if (builder.deploymentEnvironment != null) {
            globalAttributes =
                    globalAttributes.toBuilder()
                            .put(
                                    ResourceAttributes.DEPLOYMENT_ENVIRONMENT,
                                    builder.deploymentEnvironment)
                            .build();
        }
        return globalAttributes;
    }

    /** The configured "beacon" URL for the RUM library. */
    public String getBeaconEndpoint() {
        return beaconEndpoint;
    }

    /** The configured RUM access token for the library. */
    public String getRumAccessToken() {
        return rumAccessToken;
    }

    /** Is debug mode enabled. */
    public boolean isDebugEnabled() {
        return debugEnabled;
    }

    /** The name under which this application will be reported to the Splunk RUM system. */
    public String getApplicationName() {
        return applicationName;
    }

    /** Is the crash-reporting feature enabled or not. */
    public boolean isCrashReportingEnabled() {
        return crashReportingEnabled;
    }

    /** Is the slow rendering detection feature enabled or not. */
    public boolean isSlowRenderingDetectionEnabled() {
        return slowRenderingDetectionEnabled;
    }

    /**
     * The set of {@link Attributes} which will be applied to every span generated by the RUM
     * instrumentation.
     */
    public Attributes getGlobalAttributes() {
        return globalAttributes;
    }

    /** Is the network monitoring feature enabled or not. */
    public boolean isNetworkMonitorEnabled() {
        return networkMonitorEnabled;
    }

    /** Is the ANR detection feature enabled or not. */
    public boolean isAnrDetectionEnabled() {
        return anrDetectionEnabled;
    }

    /**
     * Returns the number of ms to be used for polling frame render durations, used in slow render
     * and freeze draw detection.
     *
     * @return Duration of the polling interval.
     */
    public Duration getSlowRenderingDetectionPollInterval() {
        return slowRenderingDetectionPollInterval;
    }

    /** Is the storage-based buffering of telemetry enabled or not. */
    public boolean isDiskBufferingEnabled() {
        return diskBufferingEnabled;
    }

    /**
     * Returns the max number of megabytes that will be used to buffer telemetry data in storage. If
     * this value is exceeded, older telemetry will be deleted until the usage is reduced.
     */
    public int getMaxUsageMegabytes() {
        return maxUsageMegabytes;
    }

    /** Is session-based sampling of traces enabled or not. */
    public boolean isSessionBasedSamplerEnabled() {
        return sessionBasedSamplerEnabled;
    }

    /** Get ratio of sessions that get sampled (0.0 - 1.0, where 1 is all sessions). */
    public double getSessionBasedSamplerRatio() {
        return sessionBasedSamplerRatio;
    }

    /**
     * Create a new instance of the {@link Builder} class. All default configuration options will be
     * pre-populated.
     */
    public static Builder builder() {
        return new Builder();
    }

    SplunkRumBuilder toSplunkRumBuilder() {
        SplunkRumBuilder splunkRumBuilder =
                new SplunkRumBuilder()
                        .setApplicationName(applicationName)
                        .setBeaconEndpoint(beaconEndpoint)
                        .setRumAccessToken(rumAccessToken);
        if (debugEnabled) {
            splunkRumBuilder.enableDebug();
        }
        if (diskBufferingEnabled) {
            splunkRumBuilder.enableDiskBuffering();
        }
        if (!crashReportingEnabled) {
            splunkRumBuilder.disableCrashReporting();
        }
        if (!networkMonitorEnabled) {
            splunkRumBuilder.disableNetworkMonitorEnabled();
        }
        if (!anrDetectionEnabled) {
            splunkRumBuilder.disableAnrDetection();
        }
        if (!slowRenderingDetectionEnabled) {
            splunkRumBuilder.disableSlowRenderingDetection();
        }
        splunkRumBuilder
                .setSlowRenderingDetectionPollInterval(slowRenderingDetectionPollInterval)
                .setGlobalAttributes(globalAttributes)
                .filterSpans(spanFilterBuilderConfigurer)
                .limitDiskUsageMegabytes(maxUsageMegabytes);
        if (sessionBasedSamplerEnabled) {
            splunkRumBuilder.enableSessionBasedSampling(sessionBasedSamplerRatio);
        }
        return splunkRumBuilder;
    }

    /**
     * Builder class for the Splunk RUM {@link Config} class.
     *
     * @deprecated Use {@link #builder()} and the {@link SplunkRumBuilder} to configure a {@link
     *     SplunkRum} instance.
     */
    @Deprecated
    public static class Builder {

        private static final Duration DEFAULT_SLOW_RENDERING_DETECTION_POLL_INTERVAL =
                Duration.ofSeconds(1);

        private boolean networkMonitorEnabled = true;
        private boolean anrDetectionEnabled = true;
        private boolean slowRenderingDetectionEnabled = true;
        private boolean diskBufferingEnabled = false;
        private String beaconEndpoint;
        private String rumAccessToken;
        private boolean debugEnabled = false;
        private String applicationName;
        private boolean crashReportingEnabled = true;
        private Attributes globalAttributes = Attributes.empty();
        private String deploymentEnvironment;
        private final SpanFilterBuilder spanFilterBuilder = new SpanFilterBuilder();
        private Consumer<SpanFilterBuilder> spanFilterBuilderConfigurer = f -> {};
        private String realm;
        private Duration slowRenderingDetectionPollInterval =
                DEFAULT_SLOW_RENDERING_DETECTION_POLL_INTERVAL;
        private int maxUsageMegabytes = DEFAULT_MAX_STORAGE_USE_MB;
        private boolean sessionBasedSamplerEnabled = false;
        private double sessionBasedSamplerRatio = 1.0;

        /** Create a new instance of {@link Config} from the options provided. */
        public Config build() {
            if (rumAccessToken == null || beaconEndpoint == null || applicationName == null) {
                throw new IllegalStateException(
                        "You must provide a rumAccessToken, a realm (or full beaconEndpoint), and an applicationName to create a valid Config instance.");
            }
            return new Config(this);
        }

        /**
         * Assign the "beacon" endpoint URL to be used by the RUM library.
         *
         * <p>Note that if you are using standard Splunk ingest, it is simpler to just use {@link
         * #realm(String)} and let this configuration set the full endpoint URL for you.
         *
         * @return {@code this}.
         */
        public Builder beaconEndpoint(String beaconEndpoint) {
            if (realm != null) {
                Log.w(
                        SplunkRum.LOG_TAG,
                        "Explicitly setting the beaconEndpoint will override the realm configuration.");
                realm = null;
            }
            this.beaconEndpoint = beaconEndpoint;
            return this;
        }

        /**
         * Sets the realm for the beacon to send RUM telemetry to. This should be used in place of
         * the {@link #beaconEndpoint(String)} method in most cases.
         *
         * @param realm A valid Splunk "realm"
         * @return {@code this}.
         */
        public Builder realm(String realm) {
            if (beaconEndpoint != null && this.realm == null) {
                Log.w(
                        SplunkRum.LOG_TAG,
                        "beaconEndpoint has already been set. Realm configuration will be ignored.");
                return this;
            }
            this.beaconEndpoint = "https://rum-ingest." + realm + ".signalfx.com/v1/rum";
            this.realm = realm;
            return this;
        }

        /**
         * Assign the RUM auth token to be used by the RUM library.
         *
         * @return {@code this}.
         */
        public Builder rumAccessToken(String rumAuthToken) {
            this.rumAccessToken = rumAuthToken;
            return this;
        }

        /**
         * Enable/disable debugging information to be emitted from the RUM library. This is set to
         * {@code false} by default.
         *
         * @return {@code this}.
         */
        public Builder debugEnabled(boolean enabled) {
            this.debugEnabled = enabled;
            return this;
        }

        /**
         * Enables the storage-based buffering of telemetry. By default, telemetry will be buffered
         * in memory and throttled. If this feature is enabled, telemetry is buffered in the local
         * storage until it is exported.
         *
         * @return {@code this}.
         */
        public Builder diskBufferingEnabled(boolean enabled) {
            this.diskBufferingEnabled = enabled;
            return this;
        }

        /**
         * Enable/disable the crash reporting feature. Enabled by default.
         *
         * @return {@code this}.
         */
        public Builder crashReportingEnabled(boolean enabled) {
            this.crashReportingEnabled = enabled;
            return this;
        }

        /**
         * Enable/disable the network monitoring feature. Enabled by default.
         *
         * @return {@code this}.
         */
        public Builder networkMonitorEnabled(boolean enabled) {
            this.networkMonitorEnabled = enabled;
            return this;
        }

        /**
         * Assign an application name that will be used to identify your application in the Splunk
         * RUM UI.
         *
         * @return {@code this}.
         */
        public Builder applicationName(String applicationName) {
            this.applicationName = applicationName;
            return this;
        }

        /**
         * Enable/disable the ANR detection feature. Enabled by default. If enabled, if the main
         * thread is unresponsive for 5s or more, an event including the main thread's stack trace
         * will be reported to the RUM system.
         *
         * @return {@code this}.
         */
        public Builder anrDetectionEnabled(boolean enabled) {
            this.anrDetectionEnabled = enabled;
            return this;
        }

        /**
         * Enable/disable the slow rendering detection feature. Enabled by default.
         *
         * @return {@code this}.
         */
        public Builder slowRenderingDetectionEnabled(boolean enabled) {
            slowRenderingDetectionEnabled = enabled;
            return this;
        }

        /**
         * Configures the rate at which frame render durations are polled.
         *
         * @param interval The period that should be used for polling.
         * @return {@code this}.
         */
        public Builder slowRenderingDetectionPollInterval(Duration interval) {
            if (interval.toMillis() <= 0) {
                Log.e(
                        SplunkRum.LOG_TAG,
                        "invalid slowRenderPollingDuration: " + interval + " is not positive");
                return this;
            }
            this.slowRenderingDetectionPollInterval = interval;
            return this;
        }

        /**
         * Provide a set of global {@link Attributes} that will be applied to every span generated
         * by the RUM instrumentation.
         *
         * @return {@code this}.
         */
        public Builder globalAttributes(Attributes attributes) {
            this.globalAttributes = attributes == null ? Attributes.empty() : attributes;
            return this;
        }

        /**
         * Assign the deployment environment for this RUM instance. Will be passed along as a span
         * attribute to help identify in the Splunk RUM UI.
         *
         * @param environment The deployment environment name.
         * @return {@code this}.
         */
        public Builder deploymentEnvironment(String environment) {
            this.deploymentEnvironment = environment;
            return this;
        }

        /**
         * Configure span data filtering.
         *
         * @param configurer A function that will configure the passed {@link SpanFilterBuilder}.
         * @return {@code this}.
         */
        public Builder filterSpans(Consumer<SpanFilterBuilder> configurer) {
            Consumer<SpanFilterBuilder> previous = this.spanFilterBuilderConfigurer;
            this.spanFilterBuilderConfigurer = previous.andThen(configurer);
            configurer.accept(spanFilterBuilder);
            return this;
        }

        /**
         * Sets the limit of the max number of megabytes that will be used to buffer telemetry data
         * in storage. When this value is exceeded, older telemetry will be deleted until the usage
         * is reduced.
         */
        public Builder limitDiskUsageMegabytes(int maxUsageMegabytes) {
            this.maxUsageMegabytes = maxUsageMegabytes;
            return this;
        }

        /**
         * Enable/disable session-based sampling of traces. Disabled by default.
         *
         * @return {@code this}.
         */
        public Builder sessionBasedSamplingEnabled(boolean enabled) {
            this.sessionBasedSamplerEnabled = enabled;
            return this;
        }

        /**
         * Set ratio of sessions that get sampled (0.0 - 1.0, where 1 is all sessions). Default is
         * 1.0.
         *
         * @return {@code this}.
         */
        public Builder enableSessionBasedSampling(double ratio) {
            if (ratio < 0.0) {
                Log.e(
                        SplunkRum.LOG_TAG,
                        "invalid sessionBasedSamplingRatio: " + ratio + " must not be negative");
                return this;
            } else if (ratio > 1.0) {
                Log.e(
                        SplunkRum.LOG_TAG,
                        "invalid sessionBasedSamplingRatio: "
                                + ratio
                                + " must not be greater than 1.0");
                return this;
            }

            this.sessionBasedSamplerEnabled = true;
            this.sessionBasedSamplerRatio = ratio;
            return this;
        }
    }
}
