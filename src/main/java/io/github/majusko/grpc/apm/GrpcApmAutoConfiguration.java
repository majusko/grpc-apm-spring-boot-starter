package io.github.majusko.grpc.apm;

import co.elastic.apm.opentracing.ElasticApmTracer;
import io.github.majusko.grpc.apm.interceptor.ApmClientInterceptor;
import io.opentracing.Tracer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcApmAutoConfiguration {

    @Bean
    public static Tracer elasticApmTracer() {
        return new ElasticApmTracer();
    }

    @Bean
    public static ApmClientInterceptor apmClientInterceptor() {
        return new ApmClientInterceptor(elasticApmTracer());
    }
}
