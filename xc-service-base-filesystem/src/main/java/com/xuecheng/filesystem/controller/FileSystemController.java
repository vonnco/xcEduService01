package com.xuecheng.filesystem.controller;

import com.xuecheng.api.filesystem.FileSystemControllerApi;
import com.xuecheng.filesystem.service.FileSystemService;
import com.xuecheng.framework.domain.filesystem.response.UploadFileResult;
import com.xuecheng.framework.model.response.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/filesystem")
public class FileSystemController implements FileSystemControllerApi {
    @Autowired
    private FileSystemService fileSystemService;

    @Override
    @PostMapping("/upload")
    public UploadFileResult upload(@RequestParam("file") MultipartFile multipartFile, @RequestParam("businesskey") String businesskey, @RequestParam("filetag") String filetag, @RequestParam("metadata") String metadata) {
        return fileSystemService.upload(multipartFile,businesskey,filetag,metadata);
    }

    @Override
    @DeleteMapping("/del")
    public ResponseResult delete(@RequestParam("id") String fileId) {
        return fileSystemService.delete(fileId);
    }
}
