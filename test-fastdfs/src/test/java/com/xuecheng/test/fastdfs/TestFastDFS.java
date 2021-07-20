package com.xuecheng.test.fastdfs;

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
    public void testUpload() {
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
            String filePath = "D:/help.jpg";
            //上传成功后拿到文件id
            String fileId = storageClient1.upload_file1(filePath, "jpg", null);
            System.out.println(fileId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //下载文件
    @Test
    public void testDownload() {
        FileOutputStream fileOutputStream = null;
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
            byte[] bytes = storageClient1.download_file1("group1/M00/00/00/wKgRZGDbyPKAMQJQAAAFgREPEzU763.jpg");
            fileOutputStream = new FileOutputStream(new File("F:/help.jpg"));
            fileOutputStream.write(bytes);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //查询文件
    @Test
    public void testQueryFile(){
        try {
            ClientGlobal.initByProperties("config/fastdfs-client.properties");
            TrackerClient tracker = new TrackerClient();
            TrackerServer trackerServer = tracker.getConnection();
            StorageServer storageServer = null;
            StorageClient storageClient = new StorageClient(trackerServer,storageServer);
            FileInfo fileInfo = storageClient.query_file_info("group1", "M00/00/00/wKgRZGDbyPKAMQJQAAAFgREPEzU763.jpg");
            System.out.println(fileInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //删除文件
    @Test
    public void testDeleteFile() {
        try {
            ClientGlobal.initByProperties("config/fastdfs-client.properties");
            TrackerClient tracker = new TrackerClient();
            TrackerServer trackerServer = tracker.getConnection();
            StorageServer storageServer = null;
            StorageClient storageClient = new StorageClient(trackerServer,storageServer);
            int result = storageClient.delete_file("group1", "M00/00/00/wKgRZGDcC06AX0r-AAAFgREPEzU998.jpg");
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
