package com.xuecheng.manage_cms.dao;

import com.xuecheng.framework.domain.cms.CmsTemplate;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CmsTemplateRepository extends MongoRepository<CmsTemplate,String> {
    //根据站点Id和模板名称查询模板
    CmsTemplate findBySiteIdAndTemplateName(String siteId,String templateName);
}
