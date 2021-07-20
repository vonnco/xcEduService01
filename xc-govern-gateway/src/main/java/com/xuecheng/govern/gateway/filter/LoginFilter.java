package com.xuecheng.govern.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.govern.gateway.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginFilter extends ZuulFilter {
    private static final Logger LOG = LoggerFactory.getLogger(LoginFilter.class);

    @Autowired
    private AuthService authService;

    //过滤器的类型
    @Override
    public String filterType() {
        //pre: 请求在被路由之前执行
        //routing: 在路由请求时执行
        //post: 在routing和error过滤器之后调用
        //error: 处理请求发生错误调用
        return "pre";
    }

    //过滤器的序号
    @Override
    public int filterOrder() {
        return 0;//int值来定义过滤器的执行顺序，数值越小优先级越高
    }

    //是否执行过滤器
    @Override
    public boolean shouldFilter() {
        return true;
    }

    //过滤器内容
    @Override
    public Object run() {
        //上下文对象
        RequestContext requestContext = RequestContext.getCurrentContext();
        //得到request
        HttpServletRequest request = requestContext.getRequest();
        //查询身份令牌
        String access_token = authService.getTokenFromCookie(request);
        if(access_token == null){
            //拒绝访问
            access_denied();
            return null;
        }
        //从redis中校验身份令牌是否过期
        long expire = authService.getExpire(access_token);
        if(expire<=0){
            //拒绝访问
            access_denied();
            return null;
        }
        //查询jwt令牌
        String jwt = authService.getJwtFromHeader(request);
        if(jwt == null){
            //拒绝访问
            access_denied();
            return null;
        }
        return null;
    }

    //拒绝访问
    private void access_denied(){
        //上下文对象
        RequestContext requestContext = RequestContext.getCurrentContext();
        //拒绝访问
        requestContext.setSendZuulResponse(false);
        //设置状态码
        requestContext.setResponseStatusCode(200);
        //设置响应内容
        ResponseResult responseResult =new ResponseResult(CommonCode.UNAUTHENTICATED);
        String responseResultString = JSON.toJSONString(responseResult);
        requestContext.setResponseBody(responseResultString);
        HttpServletResponse response = requestContext.getResponse();
        //设置contentType
        response.setContentType("application/json;charset=utf-8");
    }
}
