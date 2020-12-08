package com.crowdin.cli.commands.functionality;

import com.crowdin.cli.properties.helper.FileHelper;
import com.crowdin.cli.utils.PlaceholderUtil;
import com.crowdin.cli.utils.Utils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;

public class SourcesUtils {

    private static final String DOUBLED_ASTERISK = "**";
    private static final String REGEX = "regex";
    private static final String ASTERISK = "*";
    private static final String QUESTION_MARK = "?";
    private static final String DOT = ".";
    private static final String DOT_PLUS = ".+";
    private static final String SET_OPEN_BRECKET = "[";
    private static final String SET_CLOSE_BRECKET = "]";
    private static final String ROUND_BRACKET_OPEN = "(";
    private static final String ROUND_BRACKET_CLOSE = ")";
    private static final String ESCAPE_ROUND_BRACKET_OPEN = "\\(";
    private static final String ESCAPE_ROUND_BRACKET_CLOSE = "\\)";
    private static final String ESCAPE_DOT = "\\.";
    private static final String ESCAPE_DOT_PLACEHOLDER = "{ESCAPE_DOT}";
    private static final String ESCAPE_QUESTION = "\\?";
    private static final String ESCAPE_QUESTION_PLACEHOLDER = "{ESCAPE_QUESTION_MARK}";
    private static final String ESCAPE_ASTERISK = "\\*";
    private static final String ESCAPE_ASTERISK_PLACEHOLDER = "{ESCAPE_ASTERISK}";

    public static Stream<File> getFiles(String basePath, String sourcePattern, List<String> ignorePattern, PlaceholderUtil placeholderUtil) {
        if (basePath == null || sourcePattern == null || placeholderUtil == null) {
            throw new NullPointerException("null args in SourceUtils.getFiles");
        }
        FileHelper fileHelper = new FileHelper(basePath);
        List<File> sources = fileHelper.getFiles(sourcePattern);
        List<String> formattedIgnores = placeholderUtil.format(sources, ignorePattern, false);
        return fileHelper.filterOutIgnoredFiles(sources, formattedIgnores)
            .stream()
            .filter(File::isFile);
    }

    public static List<String> filterProjectFiles(
        List<String> filePaths, String sourcePattern, List<String> ignorePatterns, boolean preserveHierarchy, PlaceholderUtil placeholderUtil
    ) {
        filePaths = filePaths.stream().map(Utils::unixPath).map(Utils::noSepAtStart).collect(Collectors.toList());
        sourcePattern = Utils.noSepAtStart(Utils.unixPath(sourcePattern));
        ignorePatterns = (ignorePatterns != null)
            ? ignorePatterns.stream().map(Utils::unixPath).map(Utils::noSepAtStart).collect(Collectors.toList()) : Collections.emptyList();
        Predicate<String> sourcePredicate;
        Predicate<String> ignorePredicate;
        if (preserveHierarchy) {
            sourcePredicate = Pattern.compile("^" + translateToRegex(sourcePattern) + "$").asPredicate();
            ignorePredicate = placeholderUtil.formatForRegex(ignorePatterns, false).stream()
                .map(Pattern::compile)
                .map(Pattern::asPredicate)
                .map(Predicate::negate)
                .reduce((s) -> true, Predicate::and);
        } else {
            List<Pattern> patternPaths = Arrays.stream(sourcePattern.split("/"))
                .map(pathSplit -> Pattern.compile(translateToRegex(pathSplit)))
                .collect(Collectors.toList());
            Collections.reverse(patternPaths);
            sourcePredicate = (filePath) -> {
                List<String> filePathSplit = asList(filePath.split("/+"));
                Collections.reverse(filePathSplit);
                for (int i = 0; true; i++) {
                    if (i >= filePathSplit.size()) {
                        return true;
                    } else if (i >= patternPaths.size()) {
                        return false;
                    } else if (patternPaths.get(i).pattern().equals("^.+$")) {
                        return true;
                    } else if (!patternPaths.get(i).matcher(filePathSplit.get(i)).matches()) {
                        return false;
                    }
                }
            };
            ignorePredicate = ignorePatterns.stream()
                .map(ignorePattern -> {
                    List<String> ignorePatternPaths = placeholderUtil.formatForRegex(asList(ignorePattern.split("/")), false);
                    Collections.reverse(ignorePatternPaths);
                    return ignorePatternPaths;
                })
                .map(path -> path.stream().map(Pattern::compile).collect(Collectors.toList()))
                .map(ignorePatternPaths -> (Predicate<String>) (filePath) -> {
                    List<String> filePathSplit = asList(filePath.split("[\\\\/]+"));
                    Collections.reverse(filePathSplit);
                    for (int i = 0; true; i++) {
                        if (i > filePathSplit.size()) {
                            return true;
                        } else if (i >= ignorePatternPaths.size()) {
                            return true;
                        } else if (ignorePatternPaths.get(i).pattern().equals("^.+$")) {
                            return false;
                        } else if (i >= filePathSplit.size()) {
                            return true;
                        }
                        if (!ignorePatternPaths.get(i).matcher(filePathSplit.get(i)).matches()) {
                            return true;
                        } else if (ignorePatternPaths.size() - 1 == i) {
                            return false;
                        }
                    }
                })
                .reduce((s) -> true, Predicate::and);
        }
        return filePaths.stream()
            .filter(sourcePredicate)
            .filter(ignorePredicate)
            .map(Utils::normalizePath)
            .collect(Collectors.toList());
    }

    private static String translateToRegex(String node) {
        node = node
            .replace(ESCAPE_DOT, ESCAPE_DOT_PLACEHOLDER)
            .replace(DOT, ESCAPE_DOT)
            .replace(ESCAPE_DOT_PLACEHOLDER, ESCAPE_DOT);
        node = node
            .replace(ESCAPE_QUESTION, ESCAPE_QUESTION_PLACEHOLDER)
            .replace(QUESTION_MARK, "[^/]")//DOT)
            .replace(ESCAPE_QUESTION_PLACEHOLDER, ESCAPE_QUESTION);
        node = node
            .replace(ESCAPE_ASTERISK, ESCAPE_ASTERISK_PLACEHOLDER)
            .replace("**", ".+")
            .replace(ESCAPE_ASTERISK_PLACEHOLDER, ESCAPE_ASTERISK);
        node = node
            .replace(ESCAPE_ASTERISK, ESCAPE_ASTERISK_PLACEHOLDER)
            .replace(ASTERISK, "[^/]+")//DOT_PLUS)
            .replace(ESCAPE_ASTERISK_PLACEHOLDER, ESCAPE_ASTERISK);
        node = node
            .replace(ROUND_BRACKET_OPEN, ESCAPE_ROUND_BRACKET_OPEN);
        node = node
            .replace(ROUND_BRACKET_CLOSE, ESCAPE_ROUND_BRACKET_CLOSE);
        node = "^" + node + "$";
        return node;
    }

    public static boolean containsPattern(String sourcePattern) {
        if (sourcePattern == null) {
            return false;
        }
        return sourcePattern.contains("**")
            || sourcePattern.contains("*")
            || sourcePattern.contains("?")
            || (sourcePattern.contains("[") && sourcePattern.contains("]"))
            || (sourcePattern.contains("\\") && !Utils.isWindows());
    }

    public static String getCommonPath(List<String> sources, String basePath) {
        String commonPrefix = StringUtils.getCommonPrefix(sources.toArray(new String[0]));
        String result = commonPrefix.substring(0, commonPrefix.lastIndexOf(Utils.PATH_SEPARATOR) + 1);
        result = StringUtils.removeStart(result, basePath);
        return result;
    }

    public static boolean isFileProperties(File source) {
        return FilenameUtils.isExtension(source.getName(), "properties");
    }
}
