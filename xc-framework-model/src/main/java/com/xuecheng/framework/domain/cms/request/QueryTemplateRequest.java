package com.xuecheng.framework.domain.cms.request;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class QueryTemplateRequest {
    @ApiModelProperty("站点Id")
    private String siteId;
    @ApiModelProperty("模板文件Id")
    private String templateFileId;
    @ApiModelProperty("模板名称")
    private String templateName;
}
