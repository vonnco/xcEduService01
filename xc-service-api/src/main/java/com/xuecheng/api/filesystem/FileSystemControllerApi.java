package com.xuecheng.api.filesystem;

import com.xuecheng.framework.domain.filesystem.response.UploadFileResult;
import com.xuecheng.framework.model.response.ResponseResult;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.multipart.MultipartFile;

@Api(value="FileSystem管理接口",description = "FileSystem管理")
public interface FileSystemControllerApi {
    @ApiOperation("文件上传")
    public UploadFileResult upload(MultipartFile multipartFile,String businesskey,String filetag,String metadata);

    @ApiOperation("删除文件")
    public ResponseResult delete(String fileId);
}
