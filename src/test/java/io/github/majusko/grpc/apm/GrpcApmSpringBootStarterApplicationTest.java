package io.github.majusko.grpc.apm;

import com.google.protobuf.Empty;
import io.github.majusko.grpc.apm.interceptor.ApmClientInterceptor;
import io.github.majusko.grpc.apm.interceptor.ApmServerInterceptor;
import io.github.majusko.grpc.apm.interceptor.GrpcApmContext;
import io.github.majusko.grpc.apm.interceptor.proto.Example;
import io.github.majusko.grpc.apm.interceptor.proto.ExampleServiceGrpc;
import io.grpc.*;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.MetadataUtils;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import io.opentracing.Span;
import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;
import org.awaitility.Awaitility;
import org.junit.Rule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.grpc.Metadata.BINARY_BYTE_MARSHALLER;

@ActiveProfiles("test")
@SpringBootTest(properties = {"spring.main.allow-bean-definition-overriding=true", "spring.autoconfigure.exclude=io" +
    ".github.majusko.grpc.apm.GrpcApmAutoConfiguration"})
@Import({MyTestConfig.class})
public class GrpcApmSpringBootStarterApplicationTest {

    @Autowired
    private Tracer elasticApmTracer;

    @Autowired
    private ApmClientInterceptor apmClientInterceptor;

    @Autowired
    private ApmServerInterceptor apmServerInterceptor;

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    static ManagedChannel customChannel;

    @Test
    public void testWithoutClient() throws IOException, NoSuchFieldException, IllegalAccessException {
        final ExampleService testService = new ExampleService();
        final ManagedChannel channel = initTestServer(testService);
        final ExampleServiceGrpc.ExampleServiceBlockingStub stub = ExampleServiceGrpc.newBlockingStub(channel);
        final Empty response = stub.getExample(Example.GetExampleRequest.newBuilder().build());

        Assertions.assertNotNull(response);
        Awaitility.await().untilTrue(testService.getExecutedGetExample());

        validateSpan();
    }

    @Test
    public void testWithClient() throws IOException, NoSuchFieldException, IllegalAccessException {
        final ExampleService testService = new ExampleService();
        final ManagedChannel channel = initTestServer(testService);
        final Channel interceptedChannel = ClientInterceptors.intercept(channel, apmClientInterceptor);
        final ExampleServiceGrpc.ExampleServiceBlockingStub stub = ExampleServiceGrpc
            .newBlockingStub(interceptedChannel);
        final Empty response = stub.getExample(Example.GetExampleRequest.newBuilder().build());

        Assertions.assertNotNull(response);
        Awaitility.await().untilTrue(testService.getExecutedGetExample());

        validateSpan();
    }

    @Test
    public void testGettingActiveSpanAndBinaryHeader() throws IOException, NoSuchFieldException,
        IllegalAccessException {
        final Span span = elasticApmTracer.buildSpan("activating-some-span").start();

        elasticApmTracer.activateSpan(span);

        final ExampleService testService = new ExampleService();
        final ManagedChannel channel = initTestServer(testService);
        final Channel interceptedChannel = ClientInterceptors.intercept(channel, apmClientInterceptor);
        final ExampleServiceGrpc.ExampleServiceBlockingStub stub = ExampleServiceGrpc
            .newBlockingStub(interceptedChannel);

        final Metadata header = new Metadata();
        header.put(Metadata.Key.of("mocked-bin-header" + Metadata.BINARY_HEADER_SUFFIX, BINARY_BYTE_MARSHALLER),
            "random-value".getBytes());

        final ExampleServiceGrpc.ExampleServiceBlockingStub injectedStub = MetadataUtils.attachHeaders(stub, header);

        final Empty response = injectedStub.getExample(Example.GetExampleRequest.newBuilder().build());

        Assertions.assertNotNull(response);

        validateSpan();
    }

    @Test
    public void testWithShutDown() throws IOException, NoSuchFieldException, IllegalAccessException {
        final ExampleService testService = new ExampleService();
        customChannel = initTestServer(testService);
        final Channel interceptedChannel = ClientInterceptors.intercept(customChannel, apmClientInterceptor);
        final ExampleServiceGrpc.ExampleServiceBlockingStub stub = ExampleServiceGrpc
            .newBlockingStub(interceptedChannel);

        Status status = Status.OK;

        try {
            final Empty ignore = stub.someAction(Empty.getDefaultInstance());
        } catch (StatusRuntimeException e) {
            status = e.getStatus();
        }

        Assertions.assertEquals(Status.CANCELLED.getCode(), status.getCode());

        validateSpan();
    }

    private void validateSpan() throws NoSuchFieldException, IllegalAccessException {
        final Span span = elasticApmTracer.activeSpan();

        Assertions.assertNotNull(span);

        final Field operationName = span.getClass().getDeclaredField("operationName");
        final Field finished = span.getClass().getDeclaredField("finished");

        operationName.setAccessible(true);
        finished.setAccessible(true);

        final String spanName = (String) operationName.get(span);
        final Boolean isFinished = (Boolean) finished.get(span);

        Assertions.assertTrue(spanName.startsWith("io.github.majusko.grpc.apm"));
        Assertions.assertTrue(isFinished);
    }

    private ManagedChannel initTestServer(BindableService service) throws IOException {

        final String serverName = InProcessServerBuilder.generateName();
        final Server server = InProcessServerBuilder
            .forName(serverName).directExecutor()
            .addService(service)
            .intercept(apmServerInterceptor)
            .build().start();

        grpcCleanup.register(server);

        return grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build());
    }
}

@TestConfiguration
class MyTestConfig {
    @Bean
    @Primary
    public Tracer elasticApmTracer() {
        return new MockTracer();
    }

    @Bean
    @Primary
    public ApmClientInterceptor apmClientInterceptor() {
        return new ApmClientInterceptor(elasticApmTracer());
    }
}

@GRpcService
class ExampleService extends ExampleServiceGrpc.ExampleServiceImplBase {

    private final AtomicBoolean executedGetExample = new AtomicBoolean(false);

    @Override
    public void getExample(Example.GetExampleRequest request, StreamObserver<Empty> response) {
        final Span activeSpan = GrpcApmContext.get().orElseThrow(RuntimeException::new);

        Assertions.assertNotNull(activeSpan);

        response.onNext(Empty.getDefaultInstance());
        response.onCompleted();
        executedGetExample.set(true);
    }

    @Override
    public void listExample(Example.GetExampleRequest request, StreamObserver<Empty> response) {
        response.onNext(Empty.getDefaultInstance());
        response.onCompleted();
    }

    @Override
    public void someAction(Empty request, StreamObserver<Empty> response) {
        GrpcApmSpringBootStarterApplicationTest.customChannel.shutdown();

        response.onCompleted();
    }

    AtomicBoolean getExecutedGetExample() {
        return executedGetExample;
    }
}

