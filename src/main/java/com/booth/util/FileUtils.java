package com.booth.util;

import org.springframework.boot.system.ApplicationHome;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class FileUtils {

    public static String uploadLocal(MultipartFile file) throws IOException {
        if(file.isEmpty()){
            throw new IOException("空文件不能上传");
        }
        ApplicationHome home = new ApplicationHome(FileUtils.class);
        File source = home.getSource();
        File rootDir;

        if (source != null && source.getAbsolutePath().contains("target")) {
            // IDEA开发模式：source在target/classes，向上2层，跳出target，到项目根booth
            rootDir = source.getParentFile().getParentFile();
        } else {
            // jar包运行：直接取jar父目录
            rootDir = source.getParentFile();
        }

        // 目标：项目根/uploads/materials/
        String basePath = rootDir.getAbsolutePath() + File.separator + "uploads" + File.separator + "materials" + File.separator;

        File dir = new File(basePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String originalName = file.getOriginalFilename();
        int dotIndex = originalName.lastIndexOf(".");
        String suffix = "";
        if(dotIndex != -1){
            suffix = originalName.substring(dotIndex);
        }
        String newFileName = UUID.randomUUID() + suffix;
        File saveFile = new File(basePath + newFileName);

        file.transferTo(saveFile);
        // 返回前端访问url
        return "/uploads/materials/" + newFileName;
    }
}
