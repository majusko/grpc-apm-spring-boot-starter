# Spring boot starter for [gRPC framework](https://grpc.io/) with [Elastic APM tracer](https://www.elastic.co/products/apm) 

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.github.majusko/grpc-apm-spring-boot-starter/badge.svg)](https://search.maven.org/search?q=g:io.github.majusko)
[![Release](https://jitpack.io/v/majusko/grpc-apm-spring-boot-starter.svg)](https://jitpack.io/#majusko/grpc-apm-spring-boot-starter)
[![Build Status](https://travis-ci.com/majusko/grpc-apm-spring-boot-starter.svg?branch=master)](https://travis-ci.com/majusko/grpc-apm-spring-boot-starter)
[![Test Coverage](https://codecov.io/gh/majusko/grpc-apm-spring-boot-starter/branch/master/graph/badge.svg)](https://codecov.io/gh/majusko/grpc-apm-spring-boot-starter/branch/master)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT) [![Join the chat at https://gitter.im/grpc-apm-spring-boot-starter/community](https://badges.gitter.im/grpc-apm-spring-boot-starter/community.svg)](https://gitter.im/grpc-apm-spring-boot-starter/community?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

Extending great [LogNet gRPC Spring Boot Starter library](https://github.com/LogNet/grpc-spring-boot-starter) with APM tracer module. Easy implementation using a prepared interceptor beans ready for registration.

## Quick Start

Quick start consist only from 2 simple steps.

- If you never used [LogNet gRPC library](https://github.com/LogNet/grpc-spring-boot-starter) before, have a look on this [basic setup](https://github.com/LogNet/grpc-spring-boot-starter#4-show-case) first.
- If you never used [Elastic APM tracer](https://www.elastic.co/products/apm) before, have a look on this [basic setup](https://www.elastic.co/guide/en/apm/agent/java/1.x/setup.html) first.

#### 1. Add Maven dependency

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