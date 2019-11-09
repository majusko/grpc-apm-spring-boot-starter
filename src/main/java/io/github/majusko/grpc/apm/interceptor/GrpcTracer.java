package io.github.majusko.grpc.apm.interceptor;

import co.elastic.apm.opentracing.ElasticApmTags;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapAdapter;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class GrpcTracer {

    private static final String APM_TYPE = "request";
    private final Tracer elasticApmTracer;

    public GrpcTracer(Tracer elasticApmTracer) {
        this.elasticApmTracer = elasticApmTracer;
    }

    public Span trace(ServerCall<?, ?> call, Metadata headers) {
        final String spanName = call.getMethodDescriptor().getFullMethodName();
        final SpanContext parentContext = elasticApmTracer.extract(Format.Builtin.HTTP_HEADERS, parseHeaders(headers));
        final Span span = elasticApmTracer
            .buildSpan(spanName)
            .asChildOf(parentContext)
            .withTag(ElasticApmTags.TYPE, APM_TYPE)
            .start();

        activate(span);

        return span;
    }

    public Scope activate(Span span) {
        return elasticApmTracer.activateSpan(span);
    }

    private static TextMapAdapter parseHeaders(Metadata headers) {
        //noinspection ConstantConditions
        return new TextMapAdapter(headers.keys().stream()
            .filter($ -> !$.endsWith(Metadata.BINARY_HEADER_SUFFIX))
            .map($ -> Metadata.Key.of($, Metadata.ASCII_STRING_MARSHALLER))
            .filter(headers::containsKey)
            .collect(Collectors.toMap(Metadata.Key::originalName, headers::get)));
    }
}
