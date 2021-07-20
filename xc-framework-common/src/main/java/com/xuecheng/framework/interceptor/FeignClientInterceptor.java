package com.xuecheng.framework.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;

/**
 * Feign拦截器
 */
public class FeignClientInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate requestTemplate) {
        try {
            //使用RequestContextHolder工具获取request相关变量
            ServletRequestAttributes attributes = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            if(attributes!=null){
                //取出request
                HttpServletRequest request = attributes.getRequest();
                //取出当前请求的header，找到jwt令牌
                Enumeration<String> headerNames = request.getHeaderNames();
                if (headerNames != null) {
                    while (headerNames.hasMoreElements()) {
                        String name = headerNames.nextElement();
                        String values = request.getHeader(name);
                        if(name.equals("authorization")){
                            //System.out.println("name="+name+"values="+values);
                            //将header向下传递
                            requestTemplate.header(name, values);
                        }
                    }
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }
    }
}

