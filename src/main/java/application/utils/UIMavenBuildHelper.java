package application.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UIMavenBuildHelper {
    private final static Pattern patternJS = Pattern.compile("src=[\"'](.*?\\.js)[\"']");
    private final static Pattern patternCSS = Pattern.compile("href=[\"'](.*?\\.css)[\"']");

    public static void main(String[] args) throws Exception {
        final File basedir = new File(args[0]);
        final File uiDir = new File(basedir, "ui");
        final File resLib = new File(basedir, "src/main/resources/static");

        /* MERGE JAVASCRIPT FILES */
        final ByteArrayOutputStream bigJsLib = new ByteArrayOutputStream();
        bigJsLib.write("/* #########  IT'S GENERATED FILE. DO NOT EDIT THIS. ######### */\r\n".getBytes());
        Files.readAllLines(new File(uiDir, "index.html").toPath()).stream()
                .filter(it -> it.contains("<script ") && it.contains(".js"))
                .map(it -> {
                    final Matcher matcher = patternJS.matcher(it);
                    if (matcher.find()) {
                        return matcher.group(1);
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(it -> new File(uiDir, it.replace("./", "")))
                .forEach(it -> {
                    try {
                        bigJsLib.write(Files.readAllBytes(it.toPath()));
                        bigJsLib.write("\r\n\r\n".getBytes());
                    } catch (IOException e) {
                        System.err.println("ERROR: " + e.getMessage());
                    }
                });
        final File bigJsFile = new File(resLib, "compiled.js");
        bigJsFile.delete();
        Files.write(bigJsFile.toPath(), bigJsLib.toByteArray(), StandardOpenOption.CREATE);

        /* MERGE CSS FILES */
        final ByteArrayOutputStream bigCSSLib = new ByteArrayOutputStream();
        bigCSSLib.write("/* #########  IT'S GENERATED FILE. DO NOT EDIT THIS. ######### */\r\n".getBytes());
        Files.readAllLines(new File(uiDir, "index.html").toPath()).stream()
                .filter(it -> it.contains("stylesheet") && it.contains("text/css") && it.contains(".css"))
                .map(it -> {
                    final Matcher matcher = patternCSS.matcher(it);
                    if (matcher.find()) {
                        return matcher.group(1);
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .map(it -> new File(uiDir, it.replace("./", "")))
                .forEach(it -> {
                    try {
                        bigCSSLib.write(Files.readAllBytes(it.toPath()));
                        bigCSSLib.write("\r\n\r\n".getBytes());
                    } catch (IOException e) {
                        System.err.println("ERROR: " + e.getMessage());
                    }
                });
        final File bigCSSFile = new File(resLib, "compiled.css");
        bigCSSFile.delete();
        Files.write(bigCSSFile.toPath(), bigCSSLib.toByteArray(), StandardOpenOption.CREATE);

        /* PROCESS INDEX FILE */
        final ByteArrayOutputStream indexFileContent = new ByteArrayOutputStream();
        indexFileContent.write("<!-- #########  IT'S GENERATED FILE. DO NOT EDIT THIS. ######### -->\r\n".getBytes());
        Files.readAllLines(new File(uiDir, "index.html").toPath()).stream()
                .filter(it -> {
                    final Matcher matcherJs = patternJS.matcher(it);
                    final Matcher matcherCss = patternCSS.matcher(it);
                    return !(matcherJs.find() || matcherCss.find());
                })
                .filter(it -> !it.trim().isEmpty())
                .forEach(it -> {
                    try {
                        indexFileContent.write(it.getBytes());
                        indexFileContent.write("\r\n".getBytes());
                    } catch (IOException e) {
                        System.err.println("ERROR: " + e.getMessage());
                    }
                });
        final String indexStr = new String(indexFileContent.toByteArray());
        final int headIndex = indexStr.indexOf("</head>");
        final String fixed = indexStr.substring(0, headIndex) +
                "\t<script src=\"compiled.js\"></script>\r\n" +
                "\t<link rel=\"stylesheet\" type=\"text/css\" href=\"compiled.css\">\r\n" +
                indexStr.substring(headIndex);
        final File fixedIndexFile = new File(resLib, "index.html");
        fixedIndexFile.delete();
        Files.write(fixedIndexFile.toPath(), fixed.getBytes(), StandardOpenOption.CREATE);
    }
}
