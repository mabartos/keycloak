package org.keycloak.it.tracing;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

import java.util.List;
import java.util.logging.Logger;

public class CustomSamplerProvider implements ConfigurableSamplerProvider {
    @Override
    public Sampler createSampler(ConfigProperties configProperties) {
        return new CustomSPISampler();
    }

    @Override
    public String getName() {
        return "custom-spi-sampler";
    }

    public static class CustomSPISampler implements Sampler {
        private static final Logger logger = Logger.getLogger(CustomSPISampler.class.getName());

        @Override
        public SamplingResult shouldSample(Context context,
                                           String s,
                                           String s1,
                                           SpanKind spanKind,
                                           Attributes attributes,
                                           List<LinkData> list) {
            // Do some sampling here
            logger.info("Test Custom SPI Sampler");
            return Sampler.alwaysOn().shouldSample(context, s, s1, spanKind, attributes, list);
        }

        @Override
        public String getDescription() {
            return "custom-spi-sampler-description";
        }
    }
}
