package com.xuecheng.auth.client;

import com.xuecheng.framework.client.XcServiceList;
import com.xuecheng.framework.domain.ucenter.ext.XcUserExt;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = XcServiceList.XC_SERVICE_UCENTER)
public interface UcenterClient {
    @GetMapping("/ucenter/getuserext")
    public XcUserExt getUserExt(@RequestParam("username") String username);
}
