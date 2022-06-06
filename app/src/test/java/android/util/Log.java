package android.util;

/**
 * A simple trick to be able to call Log from unit tests without having to resort to
 * mocks or robolectric. Note that we only do that for logging because it's simple
 * and ubiquitous enough. Do not use the same trick for other Android classes and
 * instead uses robolectric/mock.
 */
@SuppressWarnings("unused")
public class Log {
    public static int d(String tag, String msg) {
        System.out.println("DEBUG:" + tag + ": " + msg);
        return 0;
    }

    public static int i(String tag, String msg) {
        System.out.println("INFO:" + tag + ": " + msg);
        return 0;
    }

    public static int w(String tag, String msg) {
        System.out.println("WARN:" + tag + ": " + msg);
        return 0;
    }

    public static int e(String tag, String msg) {
        System.out.println("ERROR:" + tag + ": " + msg);
        return 0;
    }
}