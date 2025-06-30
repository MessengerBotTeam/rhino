package org.mozilla.javascript.util;

public class OS {

    public static boolean isAndroidRuntime = "Dalvik".equals(System.getProperty("java.vm.name"));

}
