/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */

package org.elasticsearch.xpack.apm;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;

import org.elasticsearch.Version;
import org.elasticsearch.common.component.AbstractLifecycleComponent;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.plugins.TracingPlugin;
import org.elasticsearch.tasks.Task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class APMTracer extends AbstractLifecycleComponent implements TracingPlugin.Tracer {

    public static final CapturingSpanExporter CAPTURING_SPAN_EXPORTER = new CapturingSpanExporter();

    private final Map<Long, Span> taskSpans = ConcurrentCollections.newConcurrentMap();

    private volatile Tracer tracer;

    @Override
    protected void doStart() {
        SdkTracerProvider sdkTracerProvider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(CAPTURING_SPAN_EXPORTER))
            .build();

        OpenTelemetry openTelemetry = OpenTelemetrySdk.builder()
            .setTracerProvider(sdkTracerProvider)
            .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
            .build();

        tracer = openTelemetry.getTracer("elasticsearch", Version.CURRENT.toString());
        tracer.spanBuilder("startup").startSpan().end();
    }

    @Override
    protected void doStop() {}

    @Override
    protected void doClose() {}

    @Override
    public void onTaskRegistered(Task task) {
        final Tracer tracer = this.tracer;
        if (tracer != null) {
            taskSpans.computeIfAbsent(task.getId(), taskId -> {
                final Span span = tracer.spanBuilder(task.getAction()).startSpan();
                span.setAttribute("es.task.id", task.getId());
                return span;
            });
        }
    }

    @Override
    public void onTaskUnregistered(Task task) {
        final Span span = taskSpans.remove(task.getId());
        if (span != null) {
            span.end();
        }
    }

    public static class CapturingSpanExporter implements SpanExporter {

        private List<SpanData> capturedSpans = new ArrayList<>();

        public void clear() {
            capturedSpans.clear();
        }

        public List<SpanData> getCapturedSpans() {
            return List.copyOf(capturedSpans);
        }

        @Override
        public CompletableResultCode export(Collection<SpanData> spans) {
            capturedSpans.addAll(spans);
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            return CompletableResultCode.ofSuccess();
        }
    }
}
