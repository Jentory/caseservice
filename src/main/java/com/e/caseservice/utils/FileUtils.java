package com.e.caseservice.utils;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.FileCopyUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

public class FileUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

    private FileUtils() {
        //使该类不会被实例化
    }

    public static int decompressGzipFile(InputStream inputStream, File destDir) {
        int totalLength = 0;
        try {
            GZIPInputStream gzipInputStream = new GZIPInputStream(inputStream);
            totalLength += decompressTarArchive(gzipInputStream, destDir);
            inputStream.close();
        } catch (IOException e) {
            LOGGER.error("解压gzip异常", e);
        }
        return totalLength;
    }

    private static int decompressTarArchive(InputStream inputStream, File destDir) {
        TarArchiveInputStream tarInputStream = null;
        int totalLength = 0;
        try {
            tarInputStream = new TarArchiveInputStream(inputStream);
            TarArchiveEntry entry;
            //循环变量tar内所有文件和目录
            while ((entry = tarInputStream.getNextTarEntry()) != null) {
                File destFile = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    mkdirs(destFile);
                    LOGGER.debug(destFile.getAbsolutePath() + "已经存在");
                } else {
                    totalLength += decompressTarFile(tarInputStream, destFile);
                }
            }

        } catch (Exception e) {
            LOGGER.error("解压tar包过程异常", e);
        } finally {
            if (tarInputStream != null) {
                try {
                    tarInputStream.close();
                } catch (Exception e) {
                    LOGGER.error("关闭tar inputStream异常", e);
                }
            }
        }
        return totalLength;
    }

    private static int decompressTarFile(InputStream inputStream, File tarFile) {
        int fileLength = 0;

        //如果父文件夹不存在则创建
        File tarFileParent = tarFile.getParentFile();
        mkdirs(tarFileParent);

        OutputStream entryOutputStream = null;
        try {
            entryOutputStream = new FileOutputStream(tarFile);
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer, 0, 4096)) != -1) {
                entryOutputStream.write(buffer, 0, length);
                fileLength += length;
            }
        } catch (Exception e) {
            LOGGER.error("写入文件" + tarFile.getAbsolutePath() + "异常", e);
        } finally {
            if (entryOutputStream != null) {
                try {
                    entryOutputStream.close();
                } catch (Exception e) {
                    LOGGER.error("关闭文件输出流" + tarFile.getAbsolutePath() + "异常", e);
                }
            }
        }
        return fileLength;

    }

    private static void mkdirs(File file) {
        if (!file.mkdirs()) {
            LOGGER.debug(file.getAbsolutePath() + "已经存在");
        }
    }

    public static boolean deleteDir(File dir) {

        try {
            org.apache.commons.io.FileUtils.deleteDirectory(dir);
        } catch (Exception e) {
            LOGGER.error("删除文件" + dir.getAbsolutePath() + "异常", e);
            return false;
        }
        return true;
    }


    public static HttpServletResponse download(HttpServletRequest request, HttpServletResponse response, String fileName, String saveName) {
        return download(request, response, fileName, saveName, false);
    }

    public static HttpServletResponse download(HttpServletRequest request, HttpServletResponse response, String fileName, String saveName, boolean autoContentType) {

        String contentType = "application/octet-stream";
        if (autoContentType) {
            try {
                contentType = Files.probeContentType(Paths.get(fileName));
            } catch (IOException e) {
                LOGGER.warn("cannot judge content type", e);
                contentType = "application/octet-stream";
            }
        }
        BufferedInputStream bis = null;

        //得到下载文件的长度，单位为字节
        long fileLength = new File(fileName).length();

        //设置response的属性
        response.setContentType(contentType);
        if (StringUtils.isEmpty(saveName)) {
            saveName = new File(fileName).getName();
        }
        try {
            response.setHeader("Content-disposition", "attachment; filename=" + new String(saveName.getBytes("utf-8"), "ISO8859-1"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        response.setHeader("Content-Length", String.valueOf(fileLength));

        //设置输入输出流
        try {
            bis = new BufferedInputStream(new FileInputStream(fileName));
            //拷贝流
            FileCopyUtils.copy(bis, response.getOutputStream());
        } catch (FileNotFoundException e) {
            LOGGER.error("下载文件不存在", e);

        } catch (IOException e) {
            LOGGER.error("下载文件拷贝失败", e);
        }
        return response;
    }


}