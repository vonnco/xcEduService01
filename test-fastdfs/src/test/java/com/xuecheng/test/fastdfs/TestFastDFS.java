package com.xuecheng.test.fastdfs;

import org.csource.common.MyException;
import org.csource.fastdfs.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@SpringBootTest
@RunWith(SpringRunner.class)
public class TestFastDFS {

    //上传文件
    @Test
    public void testUpload(){
        try {
            //加载fastdfs-client.properties配置文件
            ClientGlobal.initByProperties("config/fastdfs-client.properties");
            //定义TrackerClient，用于请求TrackerServer
            TrackerClient trackerClient = new TrackerClient();
            //连接tracker
            TrackerServer trackerServer = trackerClient.getConnection();
            //获取Storage
            StorageServer storeStorage = trackerClient.getStoreStorage(trackerServer);
            //创建StorageClient
            StorageClient1 storageClient1 = new StorageClient1(trackerServer, storeStorage);
            //向storage上传文件
            //本地文件路径
            String filePath = "E:/logo.png";
            //上传成功后拿到文件id
            String fileId = storageClient1.upload_file1(filePath, "png", null);
            System.out.println(fileId);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
    }

    //下载文件
    @Test
    public void testDownload(){
        try {
            //加载fastdfs-client.properties配置文件
            ClientGlobal.initByProperties("config/fastdfs-client.properties");
            //定义TrackerClient，用于请求TrackerServer
            TrackerClient trackerClient = new TrackerClient();
            //连接tracker
            TrackerServer trackerServer = trackerClient.getConnection();
            //获取Storage
            StorageServer storeStorage = trackerClient.getStoreStorage(trackerServer);
            //创建StorageClient
            StorageClient1 storageClient1 = new StorageClient1(trackerServer, storeStorage);
            //下载文件
            byte[] bytes = storageClient1.download_file1("group1/M00/00/00/rBOIF16YC6-AJJsfAAA1EAlKIok549.png");
            FileOutputStream fileOutputStream = new FileOutputStream(new File("F:/logo.png"));
            fileOutputStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
    }
}
