package com.crowdin.cli.utils;

import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.powermock.api.mockito.PowerMockito;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

public class UtilsTest {

    private static MockedStatic<URLEncoder> mockedSettings;
    private static MockedStatic<System> mockedSystem;


    @Test
    void encodeURLShouldEncodeTheString() throws UnsupportedEncodingException {
        mockedSettings = mockStatic(URLEncoder.class);

        String toEncode = "test string";
        String expected = "test+string";
        when(URLEncoder.encode(anyString(),anyString())).thenReturn(expected);
        String actual = Utils.encodeURL(toEncode);
        assertEquals(expected, actual);
        mockedSettings.close();
    }

    @Test
    void proxyCredentialsWhenEnvironmentVariablesAreNotSetThenReturnEmpty() {
        System.clearProperty("http.proxyUser");
        System.clearProperty("http.proxyPassword");

        Optional<Pair<String, String>> result = Utils.proxyCredentials();

        assertEquals(Optional.empty(), result);
    }

    @Test
    void getParentDirectoryOfFile() {
        String path = "C:\\Users\\user\\Desktop\\file.txt";
        String expected = "C:\\Users\\user\\Desktop\\";
        String actual = Utils.getParentDirectory(path);
        assertEquals(expected, actual);
    }

    @Test
    void getParentDirectoryOfDirectory() {
        String path = "C:\\Users\\user\\Desktop\\test";
        String expected = "C:\\Users\\user\\Desktop\\";
        String actual = Utils.getParentDirectory(path);
        assertEquals(expected, actual);
    }

    @Test
    void proxyHostWhenPortIsNotSetThenReturnEmpty() {
        System.setProperty("http.proxyHost", "localhost");
        System.setProperty("http.proxyPort", "");

        assertEquals(Optional.empty(), Utils.proxyHost());
    }

    @Test
    void proxyHostWhenHostIsNotSetThenReturnEmpty() {
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");

        Optional<Pair<String, Integer>> result = Utils.proxyHost();

        assertEquals(Optional.empty(), result);
    }

    @Test
    void splitPathWhenPathIsFileInRootDirectoryThenReturnArrayWithTwoElements() {
        String path = "file.txt";
        String[] expected = {"file.txt"};
        String[] actual = Utils.splitPath(path);
        assertArrayEquals(expected, actual);
    }

    @Test
    void splitPathWhenPathIsRootDirectoryThenReturnArrayWithOneElement() {
        String path = "C:";
        String[] result = Utils.splitPath(path);
        assertEquals(1, result.length);
        assertEquals(path, result[0]);
    }

    @Test
    void sepAtEndWhenPathIsNullThenReturnNull() {
        assertNull(Utils.sepAtEnd(null));
    }

    @Test
    void sepAtEndWhenPathIsNotNullThenReturnPathWithSeparatorAtTheEnd() {
        String path = "path";
        String expected = "path" + Utils.PATH_SEPARATOR;
        String actual = Utils.sepAtEnd(path);
        assertEquals(expected, actual);
    }

    @Test
    void noSepAtEndWhenThereIsNoSeparatorAtTheEndThenReturnTheSameString() {
        String path = "path";
        assertEquals(path, Utils.noSepAtEnd(path));
    }

    @Test
    void noSepAtEndWhenThereIsOnlyOneSeparatorAtTheEndThenReturnTheSameString() {
        String path = "path/";
        String expected = "path";
        String actual = Utils.noSepAtEnd(path);
        assertEquals(expected, actual);
    }

    @Test
    void sepAtStartWhenPathIsNullThenReturnNull() {
        assertNull(Utils.sepAtStart(null));
    }

    @Test
    void sepAtStartWhenPathIsNotNullThenReturnPathWithSeparatorAtStart() {
        String path = "path";
        String expectedPath = SystemUtils.IS_OS_WINDOWS ? "\\path" : "/path";
        assertEquals(expectedPath, Utils.sepAtStart(path));
    }

    @Test
    void regexPathShouldEscapeBackslashes() {
        String path = "\\\\";
        String expected = "\\\\\\\\";
        String actual = Utils.regexPath(path);
        assertEquals(expected, actual);
    }

    @Test
    void regexPathShouldEscapeRoundBrackets() {
        String path = "path\\\\to\\\\file(1).txt";
        String expected = "path\\\\\\\\to\\\\\\\\file\\(1\\).txt";
        String actual = Utils.regexPath(path);
        assertEquals(expected, actual);
    }

    @Test
    void readResourceShouldThrowExceptionWhenTheResourceFileDoesNotExist() {
        assertThrows(RuntimeException.class, () -> Utils.readResource("/non-existing-file.txt"));
    }

    @Test
    void unixPathShouldReplaceAllMacSeparatorsWithUnixSeparator() {
        String path = "\\test\\path\\to\\file";
        String expected = "/test/path/to/file";
        assertEquals(expected, Utils.unixPath(path));
    }

    @Test
    void unixPathShouldReplaceAllWindowsSeparatorsWithUnixSeparator() {
        String path = "\\test\\path\\to\\file";
        String expected = "/test/path/to/file";
        assertEquals(expected, Utils.unixPath(path));
    }

    @Test
    public void testGetAppName() {
        assertNotNull(Utils.getAppName());
    }

    @Test
    public void testGetAppVersion() {
        assertNotNull(Utils.getAppVersion());
    }

    @Test
    public void testGetBaseUrl() {
        assertNotNull(Utils.getBaseUrl());
    }

    @Test
    public void testIsWindows() {
        assertEquals(SystemUtils.IS_OS_WINDOWS, Utils.isWindows());
    }

    @Test
    public void testBuildUserAgent() {
        assertNotNull(Utils.buildUserAgent());
    }

    @Test
    public void testLatestVersionUrl() {
        assertEquals("https://downloads.crowdin.com/cli/v3/version.txt", Utils.getLatestVersionUrl());
    }

    @Test
    public void testLatestVersionUrlNotNull() {
        assertNotNull(Utils.getLatestVersionUrl());
    }

    @Test
    public void testUnixPathNotNull() {
        assertNull(null);
    }

    @Test
    public void testUnixPath() {
        assertEquals("/", Utils.unixPath("\\/"));
    }

    @Test
    public void testNoSepAtStart() {
        assertEquals("abc", Utils.noSepAtStart("/abc"));
    }

    @Test
    void encodeURLShouldThrowExceptionWhenEncodingIsNotSupported() throws UnsupportedEncodingException {
        mockedSettings = mockStatic(URLEncoder.class);
        when(URLEncoder.encode(anyString(),anyString())).thenThrow(UnsupportedEncodingException.class);
        assertThrows(RuntimeException.class, () -> Utils.encodeURL("test"));
        mockedSettings.close();
    }

    @Test
    void testJoinPaths(){
        String[] paths = new String[]{"C:\\\\", "User"};
        String joinedPath = Utils.joinPaths(paths);
        assertEquals("C:\\User", joinedPath);

    }
}
