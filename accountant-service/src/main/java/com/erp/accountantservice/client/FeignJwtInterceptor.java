package com.erp.accountantservice.client;

//import feign.RequestInterceptor;
//import feign.RequestTemplate;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class FeignJwtInterceptor implements RequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(FeignJwtInterceptor.class);

    @Override
    public void apply(RequestTemplate template) {

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            log.warn("Feign Interceptor: No request context available (async call?)");
            return;
        }

        HttpServletRequest request = attributes.getRequest();
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            template.header("Authorization", authHeader);
            log.info("Feign Interceptor: JWT token forwarded");
        } else {
            log.warn("Feign Interceptor: Authorization header missing or invalid");
        }
    }
}


//@Component
//public class FeignJwtInterceptor implements RequestInterceptor {
//
//    @Override
//    public void apply(RequestTemplate template) {
//        try {
//            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
//            if (attributes != null) {
//                HttpServletRequest request = attributes.getRequest();
//                String authHeader = request.getHeader("Authorization");
//
//                if (authHeader != null && authHeader.startsWith("Bearer ")) {
//                    template.header("Authorization", authHeader);
//                    System.out.println("Feign: Forwarded JWT token to auth service");
//                } else {
//                    System.out.println("Feign: No Authorization header found");
//                }
//            }
//        } catch (Exception e) {
//            System.err.println("Feign interceptor error: " + e.getMessage());
//        }
//    }
//}