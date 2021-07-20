package com.xuecheng.auth.controller;

import com.alibaba.druid.util.StringUtils;
import com.xuecheng.api.auth.AuthControllerApi;
import com.xuecheng.auth.service.AuthService;
import com.xuecheng.framework.domain.ucenter.ext.AuthToken;
import com.xuecheng.framework.domain.ucenter.request.LoginRequest;
import com.xuecheng.framework.domain.ucenter.response.AuthCode;
import com.xuecheng.framework.domain.ucenter.response.JwtResult;
import com.xuecheng.framework.domain.ucenter.response.LoginResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.framework.utils.CookieUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
public class AuthController implements AuthControllerApi {
    @Autowired
    private AuthService authService;
    @Value("${auth.clientId}")
    private String clientId;//客户端Id
    @Value("${auth.clientSecret}")
    private String clientSecret;//客户端密码
    @Value("${auth.cookieDomain}")
    private String cookieDomain;
    @Value("${auth.cookieMaxAge}")
    private int cookieMaxAge;

    @Override
    @PostMapping("/userlogin")
    public LoginResult login(LoginRequest loginRequest) {
        //校验账号是否输入
        if(loginRequest == null || StringUtils.isEmpty(loginRequest.getUsername())){
            ExceptionCast.cast(AuthCode.AUTH_USERNAME_NONE);
        }
        //校验密码是否输入
        if(StringUtils.isEmpty(loginRequest.getPassword())){
            ExceptionCast.cast(AuthCode.AUTH_PASSWORD_NONE);
        }
        //账号
        String username = loginRequest.getUsername();
        //密码
        String password = loginRequest.getPassword();
        //申请令牌
        AuthToken authToken = authService.login(username,password,clientId,clientSecret);
        String access_token = authToken.getAccess_token();
        //将令牌保存到cookie
        this.saveCookie(access_token);
        return new LoginResult(CommonCode.SUCCESS,access_token);
    }

    //将令牌保存到cookie
    private void saveCookie(String token){
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
        //添加cookie 认证令牌，最后一个参数设置为false，表示允许浏览器获取
        CookieUtil.addCookie(response, cookieDomain, "/", "uid", token, cookieMaxAge, false);
    }

    @Override
    @PostMapping("/userlogout")
    public ResponseResult logout() {
        //获取cookie中的令牌
        String access_token = getTokenFormCookie();
        //清除redis中的token
        authService.delToken(access_token);
        //清除cookie
        clearCookie(access_token);
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //清除cookie
    private void clearCookie(String token) {
        //获取response
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
        CookieUtil.addCookie(response, cookieDomain, "/", "uid", token, 0, false);
    }

    @Override
    @GetMapping("/userjwt")
    public JwtResult userJwt() {
        //获取cookie中的令牌
        String access_token = getTokenFormCookie();
        if (access_token == null) {
            return new JwtResult(CommonCode.FAIL,null);
        }
        //根据令牌从redis查询jwt
        AuthToken authToken = authService.getUserToken(access_token);
        if(authToken == null){
            return new JwtResult(CommonCode.FAIL,null);
        }
        return new JwtResult(CommonCode.SUCCESS,authToken.getJwt_token());
    }

    private String getTokenFormCookie() {
        //获取request
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        Map<String, String> cookieMap = CookieUtil.readCookie(request, "uid");
        if (cookieMap != null && cookieMap.get("uid") != null) {
            return cookieMap.get("uid");
        }
        return null;
    }
}
