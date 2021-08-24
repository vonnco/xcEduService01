package com.xuecheng.manage_media.service;

import com.alibaba.fastjson.JSON;
import com.xuecheng.framework.domain.media.MediaFile;
import com.xuecheng.framework.domain.media.response.CheckChunkResult;
import com.xuecheng.framework.domain.media.response.MediaCode;
import com.xuecheng.framework.exception.ExceptionCast;
import com.xuecheng.framework.model.response.CommonCode;
import com.xuecheng.framework.model.response.ResponseResult;
import com.xuecheng.manage_media.config.RabbitMQConfig;
import com.xuecheng.manage_media.controller.MediaUploadController;
import com.xuecheng.manage_media.dao.MediaFileRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;

@Service
public class MediaUploadService {
    private final static Logger LOGGER = LoggerFactory.getLogger(MediaUploadController.class);

    @Autowired
    private MediaFileRepository mediaFileRepository;

    //上传文件根目录
    @Value("${xc-service-manage-media.upload-location}")
    String uploadPath;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${xc-service-manage-media.mq.routingkey-media-video}")
    private String routingkey_media_video;

    //文件上传注册(判断文件是否存在)
    public ResponseResult register(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt) {
        //获取文件目录路径
        String fileFolderPath = this.getFileFolderPath(fileMd5);
        //获取文件路径
        String filePath = this.getFilePath(fileMd5, fileExt);
        //创建文件对象
        File file = new File(filePath);
        //查询文件
        Optional<MediaFile> optional = mediaFileRepository.findById(fileMd5);
        //文件已存在抛出异常
        if (file.exists() && optional.isPresent()) {
            ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_EXIST);
        }
        //创建文件夹对象
        File fileFolder = new File(fileFolderPath);
        //如果文件夹不存在则创建文件夹
        if (!fileFolder.exists()) {
            boolean mkdirs = fileFolder.mkdirs();
            if (!mkdirs) {
                ExceptionCast.cast(MediaCode.UPLOAD_FILE_REGISTER_CREATEFOLDER_FAIL);
            }
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //校验分块文件是否存在
    public CheckChunkResult checkChunk(String fileMd5, Integer chunk, Integer chunkSize) {
        //获取分块文件夹路径
        String chunkFileFolderPath = this.getChunkFileFolderPath(fileMd5);
        //创建分块文件路径
        File file = new File(chunkFileFolderPath + chunk);
        //判断分块文件是否存在
        if (file.exists()) {
            return new CheckChunkResult(MediaCode.CHUNK_FILE_EXIST_CHECK,true);
        } else {
            return new CheckChunkResult(MediaCode.CHUNK_FILE_EXIST_CHECK,false);
        }
    }

    //上传分块文件
    public ResponseResult uploadChunk(MultipartFile file, Integer chunk, String fileMd5) {
        //获取分块文件夹路径
        String chunkFileFolderPath = this.getChunkFileFolderPath(fileMd5);
        //创建分块文件夹对象
        File chunkFileFolder = new File(chunkFileFolderPath);
        //分块文件夹不存在则创建
        if (!chunkFileFolder.exists()) {
            chunkFileFolder.mkdirs();
        }
        //上传文件
        InputStream inputStream = null;
        FileOutputStream fileOutputStream = null;
        try {
            inputStream = file.getInputStream();
            fileOutputStream = new FileOutputStream(new File(chunkFileFolderPath + chunk));
            IOUtils.copy(inputStream,fileOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error("upload chunk file fail:{}",e.getMessage());
            ExceptionCast.cast(MediaCode.CHUNK_FILE_UPLOAD_FAIL);
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //合并分块文件
    public ResponseResult mergeChunks(String fileMd5, String fileName, Long fileSize, String mimetype, String fileExt){
        //获取分块文件夹路径
        String chunkFileFolderPath = this.getChunkFileFolderPath(fileMd5);
        //创建分块文件夹对象
        File chunkFileFolder = new File(chunkFileFolderPath);
        //获取文件夹下的文件数组
        File[] files = chunkFileFolder.listFiles();
        //数组转集合
        List<File> fileList = Arrays.asList(files);
        //集合按文件名字升序排序
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (Integer.parseInt(o1.getName()) > Integer.parseInt(o2.getName())) {
                    return 1;
                }
                return -1;
            }
        });
        //获取文件路径
        String filePath = this.getFilePath(fileMd5, fileExt);
        //创建文件对象
        File mergeFile = new File(filePath);
        //合并文件
        mergeFile = this.mergeFile(mergeFile, fileList);
        //合并文件失败抛出异常
        if (mergeFile == null) {
            ExceptionCast.cast(MediaCode.MERGE_FILE_FAIL);
        }
        //校验合并后的文件是否正常
        boolean checkFileMd5 = this.checkFileMd5(mergeFile, fileMd5);
        //校验失败抛出异常
        if (!checkFileMd5) {
            ExceptionCast.cast(MediaCode.MERGE_FILE_CHECKFAIL);
        }
        //保存文件信息到数据库
        MediaFile mediaFile = new MediaFile();
        mediaFile.setFileId(fileMd5);
        mediaFile.setFileName(fileMd5+"."+fileExt);
        mediaFile.setFileOriginalName(fileName);
        mediaFile.setFilePath(fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/");
        mediaFile.setFileSize(fileSize);
        mediaFile.setUploadTime(new Date());
        mediaFile.setMimeType(mimetype);
        mediaFile.setFileType(fileExt);//状态为上传成功
        mediaFile.setFileStatus("301002");
        MediaFile save = mediaFileRepository.save(mediaFile);
        //发送消息处理视频格式
        this.sendProcessVideoMsg(save.getFileId());
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //发送视频处理消息
    public ResponseResult sendProcessVideoMsg(String mediaId){
        Optional<MediaFile> optional = mediaFileRepository.findById(mediaId);
        if (!optional.isPresent()) {
            ExceptionCast.cast(CommonCode.FAIL);
        }
        Map<String,String> map = new HashMap();
        map.put("mediaId",mediaId);
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.EX_MEDIA_PROCESSTASK,routingkey_media_video, JSON.toJSONString(map));
        } catch (AmqpException e) {
            e.printStackTrace();
            return new ResponseResult(CommonCode.FAIL);
        }
        return new ResponseResult(CommonCode.SUCCESS);
    }

    //获取文件夹路径
    private String getFileFolderPath(String fileMd5) {
        return uploadPath+fileMd5.substring(0,1)+"/"+fileMd5.substring(1,2)+"/"+fileMd5+"/";
    }

    //获取分块文件夹路径
    private String getChunkFileFolderPath(String fileMd5){
        return getFileFolderPath(fileMd5)+"chunk/";
    }

    //获取文件路径
    private String getFilePath(String fileMd5, String fileExt){
        return getFileFolderPath(fileMd5)+fileMd5+"."+fileExt;
    }

    //合并文件
    private File mergeFile(File mergeFile,List<File> fileList) {
        try {
            if (mergeFile.exists()) {
                mergeFile.delete();
            }
            mergeFile.createNewFile();
            RandomAccessFile raf_write = new RandomAccessFile(mergeFile, "rw");
            byte[] b = new byte[1024];
            for (File file : fileList) {
                RandomAccessFile raf_read = new RandomAccessFile(file, "r");
                int len = -1;
                while ((len = raf_read.read(b)) != -1) {
                    raf_write.write(b,0,len);
                }
                raf_read.close();
            }
            raf_write.close();
        } catch (IOException e) {
            e.printStackTrace();
            LOGGER.error("merge file error:{}",e.getMessage());
            return null;
        }
        return mergeFile;
    }

    //校验合并的文件是否正常
    private boolean checkFileMd5(File mergeFile,String fileMd5){
        FileInputStream fileInputStream = null;
        try {
            fileInputStream = new FileInputStream(mergeFile);
            String mergeFileMd5 = DigestUtils.md5Hex(fileInputStream);
            if (fileMd5.equalsIgnoreCase(mergeFileMd5)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fileInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
