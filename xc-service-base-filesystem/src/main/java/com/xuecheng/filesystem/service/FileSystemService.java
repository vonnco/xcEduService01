package com.xuecheng.filesystem.service;

import com.alibaba.fastjson.JSON;
import com.xuecheng.filesystem.dao.FileSystemRepository;
import com.xuecheng.framework.domain.filesystem.FileSystem;
import com.xuecheng.framework.domain.filesystem.response.FileSystemCode;
import com.xuecheng.framework.domain.filesystem.response.UploadFileResult;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import org.apache.commons.lang3.StringUtils;
import org.csource.fastdfs.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.Optional;

@Service
public class FileSystemService {

    @Value("${xuecheng.fastdfs.connect_timeout_in_seconds}")
    private Integer connect_timeout_in_seconds;
    @Value("${xuecheng.fastdfs.network_timeout_in_seconds}")
    private Integer network_timeout_in_seconds;
    @Value("${xuecheng.fastdfs.charset}")
    private String charset;
    @Value("${xuecheng.fastdfs.tracker_servers}")
    private String tracker_servers;

    @Autowired
    private FileSystemRepository fileSystemRepository;

    /**
     * 上传文件
     * @param multipartFile
     * @param businesskey
     * @param filetag
     * @param metadata
     * @return
     */
    public UploadFileResult upload(MultipartFile multipartFile, String businesskey, String filetag, String metadata){
        if (multipartFile == null){
            ExceptionCast.cast(FileSystemCode.FS_UPLOADFILE_FILEISNULL);
        }
        //第一步：上传文件到fastDFS，得到文件id
        String fileId = fdfs_upload(multipartFile);
        if (StringUtils.isEmpty(fileId)) {
            ExceptionCast.cast(FileSystemCode.FS_UPLOADFILE_SERVERFAIL);
        }
        //第二步：把文件信息存入mongodb
        FileSystem fileSystem = new FileSystem();
        fileSystem.setBusinesskey(businesskey);
        fileSystem.setFileId(fileId);
        fileSystem.setFilePath(fileId);
        fileSystem.setFileName(multipartFile.getOriginalFilename());
        fileSystem.setFileType(multipartFile.getContentType());
        fileSystem.setFiletag(filetag);
        if (StringUtils.isNoneEmpty(metadata)){
            try {
                Map map = JSON.parseObject(metadata, Map.class);
                fileSystem.setMetadata(map);
            } catch (Exception e) {
                e.printStackTrace();
                ExceptionCast.cast(FileSystemCode.FS_UPLOADFILE_METAERROR);
            }
        }
        fileSystemRepository.save(fileSystem);
        return new UploadFileResult(CommonCode.SUCCESS,fileSystem);
    }

    /**
     * 上传文件到fastDFS
     * @param multipartFile
     * @return
     */
    private String fdfs_upload(MultipartFile multipartFile){
        try {
            //初始化fastDFS
            initFdfsConfig();
            //创建trackerClient
            TrackerClient trackerClient = new TrackerClient();
            //获取trackerServer
            TrackerServer trackerServer = trackerClient.getConnection();
            //得到Storage服务器
            StorageServer storeStorage = trackerClient.getStoreStorage(trackerServer);
            //创建storageClient1来上传文件
            StorageClient1 storageClient1 = new StorageClient1(trackerServer, storeStorage);
            //得到文件字节
            byte[] bytes = multipartFile.getBytes();
            //得到文件原始名
            String originalFilename = multipartFile.getOriginalFilename();
            //得到文件扩展名
            String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
            //得到文件id
            String fileId = storageClient1.upload_file1(bytes, ext, null);
            return fileId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 初始化fastDFS
     * @return
     */
    private void initFdfsConfig(){
        try {
            //初始化tracker服务地址(多个tracker之间用英文逗号隔开)
            ClientGlobal.initByTrackers(tracker_servers);
            ClientGlobal.setG_connect_timeout(connect_timeout_in_seconds);
            ClientGlobal.setG_network_timeout(network_timeout_in_seconds);
            ClientGlobal.setG_charset(charset);
        } catch (Exception e) {
            e.printStackTrace();
            //抛出异常
            ExceptionCast.cast(FileSystemCode.FS_UPLOADFILE_INITFDFSERROR);
        }
    }

    /**
     * 删除图片
     * @param fileId
     * @return
     */
    public ResponseResult delete(String fileId) {
        Optional<FileSystem> optional = fileSystemRepository.findById(fileId);
        if (optional.isPresent()) {
            try {
                //初始化fastDFS
                initFdfsConfig();
                //创建trackerClient
                TrackerClient trackerClient = new TrackerClient();
                //获取trackerServer
                TrackerServer trackerServer = trackerClient.getConnection();
                //得到Storage服务器
                StorageServer storeStorage = trackerClient.getStoreStorage(trackerServer);
                //创建storageClient1来上传文件
                StorageClient1 storageClient1 = new StorageClient1(trackerServer, storeStorage);
                //删除图片
                int result = storageClient1.delete_file1(fileId);
                if (result == 0) {
                    fileSystemRepository.deleteById(fileId);
                    return new ResponseResult(CommonCode.SUCCESS);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new ResponseResult(CommonCode.FAIL);
    }
}
