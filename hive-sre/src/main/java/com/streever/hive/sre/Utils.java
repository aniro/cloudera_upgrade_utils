package com.streever.hive.sre;

import org.apache.commons.lang.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class Utils {

    public static String dirToPartitionSpec(String directoryPart) throws UnsupportedEncodingException {
        String[] directories = directoryPart.split("\\/");
        String[] partitionSpecs = new String[directories.length];
        int loc = 0;
        for (String directory: directories) {
            String[] specParts = directory.split("=");
            String partDir = null;
            partDir = URLDecoder.decode(specParts[1], StandardCharsets.UTF_8.toString());
            partitionSpecs[loc++] = specParts[0] + "=\"" + partDir + "\"";
        }
        StringBuilder rtn = new StringBuilder();
        rtn.append(StringUtils.join(partitionSpecs, ","));
        return rtn.toString();
    }
}
