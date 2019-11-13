package io.github.majusko.grpc.apm.interceptor;

import io.opentracing.Span;

import java.util.Optional;

public class GrpcApmContext {

    private GrpcApmContext() {
    }

    private static final String APM_ACTIVE_SPAN = "apm_active_span";

    public static io.grpc.Context.Key<Span> ACTIVE_SPAN_KEY = io.grpc.Context.key(APM_ACTIVE_SPAN);

    public static Optional<Span> get() {
        return Optional.ofNullable(ACTIVE_SPAN_KEY.get());
    }
}
