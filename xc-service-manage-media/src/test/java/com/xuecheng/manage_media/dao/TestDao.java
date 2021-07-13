package com.xuecheng.manage_media.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class TestDao {

    //测试文件分块
    @Test
    public void testChunk() throws IOException {
        //源文件
        File sourceFile = new File("D:/code/xcEduUI01/video/lucene.avi");
        //分块文件存储路径
        String chunkPath = "D:/code/xcEduUI01/video/chunk/";
        File chunkFolder = new File(chunkPath);
        //路径为空则创建
        if (!chunkFolder.exists()) {
            chunkFolder.mkdir();
        }
        //分块大小
        long chunkSize = 1*1024*1024;
        //分块数量
        long chunkNum = (long) Math.ceil(sourceFile.length() * 1.0/chunkSize);
        //缓冲区大小
        byte[] b = new byte[1024];
        //使用RandomAccessFile访问文件（只读）
        RandomAccessFile raf_read = new RandomAccessFile(sourceFile, "r");
        //分块上传
        for (int i = 0; i < chunkNum; i++) {
            //创建分块文件
            File file = new File(chunkPath + i);
            //文件不存在则创建文件
            if (!file.exists()) {
                file.createNewFile();

            }
            //使用RandomAccessFile访问文件（读写）
            RandomAccessFile raf_write = new RandomAccessFile(file, "rw");
            int len = -1;
            while ((len = raf_read.read(b)) != -1) {
                raf_write.write(b,0,len);
                //文件写入大小大于分块大小后跳出循环
                if (raf_write.length()>chunkSize) {
                    break;
                }
            }
            raf_write.close();
        }
        raf_read.close();
    }

    //测试分块文件合并
    @Test
    public void testMerge() throws IOException {
        //分块文件所在路径
        File sourceFile = new File("D:/code/xcEduUI01/video/chunk/");
        //合并后的文件
        File mergeFile = new File("D:/code/xcEduUI01/video/lucene_merge.avi");
        //获取分块文件列表
        File[] files = sourceFile.listFiles();
        //文件数组转成集合，便于排序
        List<File> fileList = Arrays.asList(files);
        //文件按文件名升序排序
        Collections.sort(fileList, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                if (Integer.parseInt(o1.getName())>Integer.parseInt(o1.getName())) {
                    return 1;
                }
                return -1;
            }
        });
        //缓冲区大小
        byte[] b = new byte[1024];
        //使用RandomAccessFile访问文件（读写）
        RandomAccessFile raf_write = new RandomAccessFile(mergeFile, "rw");
        //合并文件
        for (File file : fileList) {
            //使用RandomAccessFile访问文件（只读）
            RandomAccessFile raf_read = new RandomAccessFile(file, "r");
            int len = -1;
            while ((len = raf_read.read(b)) != -1) {
                raf_write.write(b,0,len);
            }
            raf_read.close();
        }
        raf_write.close();
    }
}
