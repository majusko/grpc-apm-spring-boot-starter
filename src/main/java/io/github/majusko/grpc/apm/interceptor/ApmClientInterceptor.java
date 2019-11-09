package io.github.majusko.grpc.apm.interceptor;

import io.grpc.*;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;

public class ApmClientInterceptor implements ClientInterceptor {

    private final Tracer elasticApmTracer;

    public ApmClientInterceptor(Tracer elasticApmTracer) {
        this.elasticApmTracer = elasticApmTracer;
    }

    private TextMap addHeaders(final Metadata headers) {
        return new TextMap() {
            @Override
            public void put(String key, String value) {
                Metadata.Key<String> headerKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
                headers.put(headerKey, value);
            }

            @Override
            public Iterator<Map.Entry<String, String>> iterator() {
                throw new UnsupportedOperationException("TextMapAdapter should only be used with Tracer.inject()");
            }
        };
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
        MethodDescriptor<ReqT, RespT> method,
        CallOptions callOptions,
        Channel next
    ) {

        final String operationName = method.getFullMethodName();
        final Span span = elasticApmTracer.buildSpan(operationName).asChildOf(elasticApmTracer.activeSpan()).start();

        try(Scope ignored = elasticApmTracer.activateSpan(span)) {
            return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(next.newCall(method, callOptions)) {

                @Override
                public void start(Listener<RespT> responseListener, final Metadata headers) {

                    elasticApmTracer.inject(span.context(), Format.Builtin.HTTP_HEADERS, addHeaders(headers));

                    final Listener<RespT> tracingResponseListener = new ForwardingClientCallListener
                        .SimpleForwardingClientCallListener<RespT>(responseListener) {
                            @Override
                            public void onClose(Status status, Metadata metadata) {
                                super.onClose(status, metadata);
                                span.finish();
                            }
                        };

                    try(Scope ignored = elasticApmTracer.scopeManager().activate(span)) {
                        super.start(tracingResponseListener, headers);
                    }
                }

                @Override
                public void sendMessage(ReqT message) {
                    try(Scope ignored = elasticApmTracer.scopeManager().activate(span)) {
                        super.sendMessage(message);
                    }
                }

                @Override
                public void halfClose() {
                    try(Scope ignored = elasticApmTracer.scopeManager().activate(span)) {
                        super.halfClose();
                    }
                }

                @Override
                public void cancel(@Nullable String message, @Nullable Throwable cause) {
                    try(Scope ignored = elasticApmTracer.scopeManager().activate(span)) {
                        super.cancel(message, cause);
                    } finally {
                        span.finish();
                    }
                }
            };
        }
    }
}
