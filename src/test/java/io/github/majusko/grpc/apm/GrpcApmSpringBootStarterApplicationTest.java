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
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import io.opentracing.Span;
import org.awaitility.Awaitility;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lognet.springboot.grpc.GRpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class GrpcApmSpringBootStarterApplicationTest {

	@Autowired
	private ApmClientInterceptor apmClientInterceptor;

	@Autowired
	private ApmServerInterceptor apmServerInterceptor;

	@Rule
	public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

	@Test
	public void testWithoutClient() throws IOException {
		final ExampleService testService = new ExampleService();
		final ManagedChannel channel = initTestServer(testService);
		final ExampleServiceGrpc.ExampleServiceBlockingStub stub = ExampleServiceGrpc.newBlockingStub(channel);
		final Empty response = stub.getExample(Example.GetExampleRequest.newBuilder().build());

		Assert.assertNotNull(response);
		Awaitility.await().untilTrue(testService.getExecutedGetExample());
	}

	@Test
	public void testWithClient() throws IOException {
		final ExampleService testService = new ExampleService();
		final ManagedChannel channel = initTestServer(testService);
		final Channel interceptedChannel = ClientInterceptors.intercept(channel, apmClientInterceptor);
		final ExampleServiceGrpc.ExampleServiceBlockingStub stub = ExampleServiceGrpc
			.newBlockingStub(interceptedChannel);
		final Empty response = stub.getExample(Example.GetExampleRequest.newBuilder().build());

		Assert.assertNotNull(response);
		Awaitility.await().untilTrue(testService.getExecutedGetExample());
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

@GRpcService
class ExampleService extends ExampleServiceGrpc.ExampleServiceImplBase {

	private final AtomicBoolean executedGetExample = new AtomicBoolean(false);
	private final AtomicBoolean executedListExample = new AtomicBoolean(false);

	@Override
	public void getExample(Example.GetExampleRequest request, StreamObserver<Empty> response) {
		final Span activeSpan = GrpcApmContext.get().orElseThrow(RuntimeException::new);

		Assert.assertNotNull(activeSpan);

		response.onNext(Empty.getDefaultInstance());
		response.onCompleted();
		executedGetExample.set(true);
	}

	@Override
	public void listExample(Example.GetExampleRequest request, StreamObserver<Empty> response) {

		response.onNext(Empty.getDefaultInstance());
		response.onCompleted();
		executedListExample.set(true);
	}

	AtomicBoolean getExecutedGetExample() {
		return executedGetExample;
	}

	AtomicBoolean getExecutedListExample() {
		return executedListExample;
	}
}