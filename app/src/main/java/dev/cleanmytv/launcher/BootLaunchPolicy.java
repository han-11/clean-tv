package dev.cleanmytv.launcher;

final class BootLaunchPolicy {
    static final long WINDOW_MS = 5 * 60 * 1000L;

    static boolean eligible(boolean enabled, int boot, int handledBoot, long uptime) {
        return enabled && boot >= 0 && boot != handledBoot
            && uptime >= 0 && uptime < WINDOW_MS;
    }
}
