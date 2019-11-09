package io.github.majusko.grpc.apm;

import co.elastic.apm.opentracing.ElasticApmTracer;
import io.opentracing.Tracer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GrpcJwtProperties.class)
public class GrpcApmAutoConfiguration {

    private final GrpcJwtProperties grpcJwtProperties;

    public GrpcApmAutoConfiguration(GrpcJwtProperties grpcJwtProperties) {
        this.grpcJwtProperties = grpcJwtProperties;
    }

    @Bean
    public static Tracer elasticApmTracer() {
        return new ElasticApmTracer();
    }
}
