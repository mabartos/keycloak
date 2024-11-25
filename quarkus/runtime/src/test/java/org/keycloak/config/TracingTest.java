package org.keycloak.config;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.sdk.trace.ReadableSpan;
import org.junit.Test;
import org.keycloak.quarkus.runtime.tracing.OTelTracingProvider;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

public class TracingTest {

    @Test
    public void testInnerSpans() {
        var tracing = new OTelTracingProvider(GlobalOpenTelemetry.get());

        tracing.trace(TracingTest.class, "test1", span -> {
            assertThat(span, notNullValue());
            assertThat(span, is(tracing.getCurrentSpan()));
            assertThat(span.isRecording(), is(true));

            assertThat(span instanceof ReadableSpan, is(true));
            var spanData = ((ReadableSpan) span).toSpanData();
            assertThat(spanData.getName(), is("TracingTest.test1"));

            System.err.println("SPAN1");
            System.err.println(span);
            System.err.println("SPAN1 - data");
            System.err.println(spanData);

            tracing.trace(TracingTest.class, "test2", span2 -> {
                assertThat(span2, notNullValue());
                assertThat(span2, is(tracing.getCurrentSpan()));
                assertThat(span2.isRecording(), is(true));

                assertThat(span2 instanceof ReadableSpan, is(true));
                var spanData2 = ((ReadableSpan) span2).toSpanData();
                assertThat(spanData2.getName(), is("TracingTest.test2"));

                System.err.println("SPAN2");
                System.err.println(span2);
                System.err.println("SPAN2 - data");
                System.err.println(spanData2);

                var parent = ((ReadableSpan) span2).getParentSpanContext();
                assertThat(parent.getSpanId(), is(span.getSpanContext().getSpanId()));
            });
        });
    }

    @Test
    public void innerSpansNotEnded() {
        var tracing = new OTelTracingProvider(GlobalOpenTelemetry.get());

        tracing.trace(TracingTest.class, "test1", span -> {
            assertThat(span, notNullValue());
            assertThat(span, is(tracing.getCurrentSpan()));
            assertThat(span.isRecording(), is(true));

            assertThat(span instanceof ReadableSpan, is(true));
            var spanData = ((ReadableSpan) span).toSpanData();
            assertThat(spanData.getName(), is("TracingTest.test1"));

            var span2 = tracing.startSpan(TracingTest.class, "test2");
            try {
                assertThat(span2, notNullValue());
                assertThat(span2, is(tracing.getCurrentSpan()));
                assertThat(span2.isRecording(), is(true));

                assertThat(span2 instanceof ReadableSpan, is(true));
                var spanData2 = ((ReadableSpan) span2).toSpanData();
                assertThat(spanData2.getName(), is("TracingTest.test2"));

                var parent = ((ReadableSpan) span2).getParentSpanContext();
                assertThat(parent.getSpanId(), is(span.getSpanContext().getSpanId()));
            } finally {
                // not ended
            }
        });
    }
}
