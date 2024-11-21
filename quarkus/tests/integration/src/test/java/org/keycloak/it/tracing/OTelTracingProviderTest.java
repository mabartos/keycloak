/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.it.tracing;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.internal.data.ExceptionEventData;
import io.opentelemetry.semconv.ExceptionAttributes;
import io.quarkus.test.junit.main.Launch;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.utils.KeycloakDistribution;
import org.keycloak.quarkus.runtime.tracing.OTelTracingProvider;
import org.keycloak.tracing.TracingProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

@DistributionTest(keepAlive = true)
public class OTelTracingProviderTest {
    private TracingProvider tracing;

    @Inject
    OpenTelemetry otel;

    @BeforeAll
    public static void setUp2(KeycloakDistribution dist) {
        //dist.run("start-dev", "--tracing-enabled=true", "--features=opentelemetry");

    }

    @BeforeEach
    public void setTracingProvider() {
        System.err.println(otel);
        System.err.println(otel.getTracerProvider());
        this.tracing = new OTelTracingProvider();
        assertThat(GlobalOpenTelemetry.get().getTracerProvider(), is(not(OpenTelemetry.noop().getTracerProvider())));
    }

    @AfterEach
    public void endSpan() {
        if (!Span.getInvalid().equals(tracing.getCurrentSpan())) {
            tracing.endSpan();
            throw new AssertionError("Some spans were not ended. Do the proper cleanup!");
        }
    }

    @Test
    public void emptySpan() {
        var current = tracing.getCurrentSpan();
        assertThat(current, notNullValue());
        assertThat(current, is(Span.getInvalid()));

        var spanCtx = current.getSpanContext();
        assertThat(spanCtx, notNullValue());

        assertThat(spanCtx.isValid(), is(false));
        assertThat(spanCtx.getTraceFlags(), is(TraceFlags.getDefault()));
        assertThat(spanCtx.isRemote(), is(false));
    }

    @Test
    public void differentTracer() {
        var tracer1 = tracing.getTracer("tracer1");
        var tracer2 = tracing.getTracer("tracer2");
        var tracer3 = tracing.getTracer("tracer1");

        assertThat(tracer1, notNullValue());
        assertThat(tracer2, notNullValue());
        assertThat(tracer3, notNullValue());

        assertThat(tracer1.equals(tracer2), is(false));
        assertThat(tracer2.equals(tracer3), is(false));
        assertThat(tracer3.equals(tracer1), is(true));

        var tracerOldVersion = tracing.getTracer("tracer1", "25.0.0");
        var tracerNewVersion = tracing.getTracer("tracer1", "26.0.0");

        assertThat(tracerOldVersion, notNullValue());
        assertThat(tracerNewVersion, notNullValue());

        assertThat(tracerOldVersion.equals(tracerNewVersion), is(false));
    }

    @Test
    public void sameSpan() {
        var span = tracing.startSpan("MyTracer", "sameSpan");
        try {
            var current = tracing.getCurrentSpan();
            assertThat(current, notNullValue());
            assertThat(current.equals(span), is(true));

            var context = current.getSpanContext();
            assertThat(context, is(span.getSpanContext()));
            assertThat(context.getTraceFlags(), is(TraceFlags.getSampled()));
            assertThat(current.isRecording(), is(true));

            assertThat(current instanceof ReadableSpan, is(true));
            var readableSpan = (ReadableSpan) current;
            assertThat(readableSpan.getName(), is("sameSpan"));
        } finally {
            tracing.endSpan();
        }

        assertThat(span.isRecording(), is(false));

        var current = tracing.getCurrentSpan();
        assertThat(current, is(not(span)));
    }

    @Test
    public void errorInSpan() {
        try {
            var span = tracing.startSpan("MyTracer", "something");
            try {
                var current = tracing.getCurrentSpan();
                assertThat(current, notNullValue());
                assertThat(current.equals(span), is(true));
                throw new RuntimeException("something bad happened");
            } catch (Exception e) {
                tracing.error(e);
            } finally {
                // not ended span here
            }

            var current = tracing.getCurrentSpan();
            assertThat(current, is(span));

            assertThat(current instanceof ReadableSpan, is(true));
            var spanData = ((ReadableSpan) current).toSpanData();
            assertThat(spanData.getName(), is("something"));
            assertThat(spanData.getTotalRecordedEvents(), is(1));

            var eventData = spanData.getEvents().get(0);
            assertThat(eventData instanceof ExceptionEventData, is(true));

            var exceptionData = (ExceptionEventData) eventData;
            var exceptionAttributes = exceptionData.getAttributes();
            assertThat(exceptionAttributes, notNullValue());

            assertThat(exceptionAttributes.get(ExceptionAttributes.EXCEPTION_ESCAPED), is(true));
            assertThat(exceptionAttributes.get(ExceptionAttributes.EXCEPTION_MESSAGE), is("something bad happened"));
            assertThat(exceptionAttributes.get(ExceptionAttributes.EXCEPTION_STACKTRACE), not(emptyOrNullString()));
            assertThat(exceptionAttributes.get(ExceptionAttributes.EXCEPTION_TYPE), is(RuntimeException.class.getCanonicalName()));
        } finally {
            tracing.endSpan();
        }
    }

    @Test
    public void traceSuccessful() {
        tracing.trace(OpenTelemetry.class, "successful", span -> {
            assertThat(span, notNullValue());
            assertThat(span, is(tracing.getCurrentSpan()));
            assertThat(span.isRecording(), is(true));

            assertThat(span instanceof ReadableSpan, is(true));
            var spanData = ((ReadableSpan) span).toSpanData();
            assertThat(spanData.getName(), is("OpenTelemetry.successful"));
        });
    }

    @Test
    public void traceSuccessfulValue() {
        var spanName = tracing.trace(OpenTelemetry.class, "successful", (span) -> {
            assertThat(span, notNullValue());
            assertThat(span, is(tracing.getCurrentSpan()));
            assertThat(span.isRecording(), is(true));

            assertThat(span instanceof ReadableSpan, is(true));
            var spanData = ((ReadableSpan) span).toSpanData();
            assertThat(spanData.getName(), is("OpenTelemetry.successful"));

            return spanData.getName();
        });

        assertThat(spanName, notNullValue());
        assertThat(spanName, is("OpenTelemetry.successful"));
    }

    @Test
    public void traceWithError() {
        try {
            tracing.trace(OTelTracingProviderTest.class, "errorSpan", span -> {
                assertThat(span, notNullValue());
                assertThat(span, is(tracing.getCurrentSpan()));
                assertThat(span.isRecording(), is(true));

                assertThat(span instanceof ReadableSpan, is(true));
                var spanData = ((ReadableSpan) span).toSpanData();
                assertThat(spanData.getName(), is("OTelTracingProviderTest.errorSpan"));

                if (true) {
                    throw new IllegalStateException("some invalid error");
                }
            });
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), is("some invalid error"));
            assertThat(tracing.getCurrentSpan(), is(Span.getInvalid()));
            return;
        }
        throw new AssertionError("The IllegalStateException was not propagated");
    }
}
