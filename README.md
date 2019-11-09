# Spring boot starter for [gRPC framework](https://grpc.io/) with [Elastic APM tracer](https://elastic.org/) 

[![Release](https://jitpack.io/v/majusko/grpc-apm-spring-boot-starter.svg)](https://jitpack.io/#majusko/grpc-apm-spring-boot-starter)
[![Build Status](https://travis-ci.com/majusko/grpc-apm-spring-boot-starter.svg?branch=master)](https://travis-ci.com/majusko/grpc-apm-spring-boot-starter)
[![Test Coverage](https://codecov.io/gh/majusko/grpc-apm-spring-boot-starter/branch/master/graph/badge.svg)](https://codecov.io/gh/majusko/grpc-apm-spring-boot-starter/branch/master)

Extending great [LogNet gRPC Spring Boot Starter library](https://github.com/LogNet/grpc-spring-boot-starter) with APM tracer module. Easy implementation using a prepared interceptor beans ready for registration.

## Quick Start

Quick start consist only from 2 simple steps.

(If you never used [gRPC library](https://github.com/LogNet/grpc-spring-boot-starter) before, have a look on this [basic setup](https://github.com/LogNet/grpc-spring-boot-starter#4-show-case) first.)

#### 1. Add Maven dependency

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

```xml
<dependency>
  <groupId>io.github.majusko</groupId>
  <artifactId>grpc-apm-spring-boot-starter</artifactId>
  <version>${version}</version>
</dependency>
```

#### 2. Add interceptor to client

Just autowire already prepared `ApmClientInterceptor` bean and intercept your client. Every request using this client is traced from now on.

```java
@Service
public class ExampleClient {

    @Autowired
    private ApmClientInterceptor apmClientInterceptor;

    public void exampleRequest() {
        final ManagedChannel channel = ManagedChannelBuilder.forTarget(target).usePlaintext().build();
        final Channel interceptedChannel = ClientInterceptors.intercept(channel, apmClientInterceptor);
        final ExampleServiceBlockingStub stub = ExampleServiceGrpc.newBlockingStub(interceptedChannel);
        
        stub.getExample(GetExample.newBuilder().build());
    }
}
```

### Tests

The library is fully covered with integration tests which are also very useful as a usage example.

`GrpcApmSpringBootStarterApplicationTest`

## Contributing

All contributors are welcome. If you never contributed to the open-source, start with reading the [Github Flow](https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/github-flow).

1. Create an [issue](https://help.github.com/en/github/managing-your-work-on-github/about-issues)
2. Create a [pull request](https://help.github.com/en/github/collaborating-with-issues-and-pull-requests/about-pull-requests) with reference to the issue
3. Rest and enjoy the great feeling of being a contributor.