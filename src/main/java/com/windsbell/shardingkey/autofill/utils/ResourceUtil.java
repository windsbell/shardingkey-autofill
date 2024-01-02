package com.windsbell.shardingkey.autofill.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResourceUtil {

    /**
     * 获取resource目录下某个文件夹的所有文件路径
     *
     * @param folderPath 文件夹路径
     * @return 文件路径的List
     */
    public static Map<String, String> getResourceFilesContent(String folderPath) {
        Map<String, String> fileContentsMap = new LinkedHashMap<>();
        try {
            // 获取resource目录下某个文件夹的路径
            List<String> fileNames = getResourceFiles(folderPath);
            // 读取每个文件的内容并存储到List中
            for (String fileName : fileNames) {
                String content = readFileContent(folderPath + "/" + fileName);
                fileContentsMap.put(fileName, content);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileContentsMap;
    }


    /**
     * 获取resource目录下某个文件夹的所有文件路径
     *
     * @param folderPath 文件夹路径
     * @return 文件路径的List
     * @throws IOException
     */
    public static List<String> getResourceFiles(String folderPath) throws IOException {
        List<String> fileNames = new ArrayList<>();
        // 使用ClassLoader获取资源文件夹的URL
        InputStream inputStream = ResourceUtil.class.getClassLoader().getResourceAsStream(folderPath);
        if (inputStream == null) {
            throw new IllegalArgumentException("Folder not found! Path: " + folderPath);
        }
        // 使用BufferedReader逐行读取URL中的文件名
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String fileName;
        while ((fileName = reader.readLine()) != null) {
            fileNames.add(fileName);
        }
        return fileNames;
    }


    /**
     * 读取文件的内容
     *
     * @param filePath 文件路径
     * @return 文件的内容字符串
     * @throws IOException
     */
    public static String readFileContent(String filePath) throws IOException {
        // 使用ClassLoader获取资源文件的URL
        InputStream inputStream = ResourceUtil.class.getClassLoader().getResourceAsStream(filePath);
        if (inputStream == null) {
            throw new IllegalArgumentException("File not found! Path: " + filePath);
        }
        // 使用BufferedReader逐行读取文件内容并拼接成字符串
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder contentBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            contentBuilder.append(line).append("\n");
        }
        return contentBuilder.toString();
    }

}
