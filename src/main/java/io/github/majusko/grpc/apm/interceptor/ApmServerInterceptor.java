package io.github.majusko.grpc.apm.interceptor;

import io.grpc.*;
import io.opentracing.Scope;
import io.opentracing.Span;
import org.lognet.springboot.grpc.GRpcGlobalInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@GRpcGlobalInterceptor
public class ApmServerInterceptor implements ServerInterceptor {

    private static final ServerCall.Listener NOOP_LISTENER = new ServerCall.Listener() {
    };

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final GrpcTracer grpcTracer;

    public ApmServerInterceptor(GrpcTracer grpcTracer) {
        this.grpcTracer = grpcTracer;
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> call, Metadata metadata, ServerCallHandler<ReqT, RespT> next
    ) {
        final Span span = grpcTracer.trace(call, metadata);

        try {
            final Context context = Context.current().withValue(GrpcApmContext.ACTIVE_SPAN_KEY, span);

            return buildListener(call, metadata, next, context, span);
        } catch(Exception e) {
            logger.error(e.getMessage(), e);
            span.finish();
            //noinspection unchecked
            return NOOP_LISTENER;
        }
    }

    private <ReqT, RespT> ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> buildListener(
        ServerCall<ReqT, RespT> call,
        Metadata metadata,
        ServerCallHandler<ReqT, RespT> next,
        Context context,
        Span span
    ) {
        final ServerCall.Listener<ReqT> customDelegate = Contexts.interceptCall(context, call, metadata, next);
        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(customDelegate) {

            @Override
            public void onMessage(ReqT request) {
                try(Scope ignored = grpcTracer.activate(span)) {
                    super.onMessage(request);
                }
            }

            @Override
            public void onHalfClose() {
                try(Scope ignored = grpcTracer.activate(span)) {
                    super.onHalfClose();
                }
            }

            @Override
            public void onCancel() {
                try(Scope ignored = grpcTracer.activate(span)) {
                    super.onCancel();
                } finally {
                    span.finish();
                }
            }

            @Override
            public void onComplete() {
                try(Scope ignored = grpcTracer.activate(span)) {
                    super.onComplete();
                } finally {
                    span.finish();
                }
            }

            @Override
            public void onReady() {
                try(Scope ignored = grpcTracer.activate(span)) {
                    super.onReady();
                }
            }
        };
    }
}