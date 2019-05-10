package com.xuecheng.manage_cms.dao;

import com.xuecheng.manage_cms.service.CmsPageService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CmsPageServiceTest {
    @Autowired
    private CmsPageService cmsPageService;

    @Test
    public void testGetPageHtml(){
        String pageHtml = cmsPageService.getPageHtml("5ccfcf902047c33bf84520fb");
        System.out.println(pageHtml);
    }
}
