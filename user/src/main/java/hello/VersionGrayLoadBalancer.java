package hello;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.*;
import org.springframework.cloud.loadbalancer.core.NoopServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class VersionGrayLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private static final Logger log = LoggerFactory.getLogger(VersionGrayLoadBalancer.class);

    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

    private final String serviceId;

    private final AtomicInteger position;

    public VersionGrayLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider, String serviceId) {
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
        this.serviceId = serviceId;
        this.position = new AtomicInteger(new Random().nextInt(1000));
    }

    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        var supplier = this.serviceInstanceListSupplierProvider.getIfAvailable(NoopServiceInstanceListSupplier::new);
        return supplier.get(request).next().map(serviceInstances -> processInstanceResponse(serviceInstances, request));
    }

    private Response<ServiceInstance> processInstanceResponse(List<ServiceInstance> instances, Request request) {
        if (instances.isEmpty()) {
            log.info("No servers available for service: " + this.serviceId);
            return new EmptyResponse();
        } else {
            DefaultRequestContext requestContext = (DefaultRequestContext) request.getContext();
            RequestData clientRequest = (RequestData)requestContext.getClientRequest();
            HttpHeaders headers = clientRequest.getHeaders();
            var dealerId = headers.getFirst("dealer_id");
            log.info("request header dealer_id : {}", dealerId);
            if ("specific-dealer-id".equals(dealerId)) {
                return processRibbonInstanceResponse(instances.stream().filter(serviceInstance -> "say-hello1".equals(
                        serviceInstance.getInstanceId())).collect(Collectors.toList()));
            }
            else {
                return processRibbonInstanceResponse(instances.stream().filter(serviceInstance -> !"say-hello1".equals(
                        serviceInstance.getInstanceId())).collect(Collectors.toList()));
            }
        }
    }

    private Response<ServiceInstance> processRibbonInstanceResponse(List<ServiceInstance> instances) {
        int pos = Math.abs(this.position.incrementAndGet());
        ServiceInstance instance = instances.get(pos % instances.size());
        return new DefaultResponse(instance);
    }
}
