package com.RIYAZSRC;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.PowerManager;
import android.os.Process;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * User-authorized foreground-app monitor. Reads Android usage events only:
 * never reads PUBG memory, modifies packages, or requests accessibility access.
 */
final class GameRuntime {
    static final String[][] GAMES = {
        {"PUBG MOBILE • GLOBAL", "com.tencent.ig"},
        {"PUBG MOBILE • KOREA", "com.pubg.krmobile"},
        {"PUBG MOBILE • VIETNAM", "com.vng.pubgmobile"},
        {"BGMI • INDIA", "com.pubg.imobile"},
        {"PUBG MOBILE • TAIWAN", "com.rekoo.pubgm"}
    };
    private static final Set<String> PACKAGES = new HashSet<>(
        Arrays.asList("com.tencent.ig", "com.pubg.krmobile",
            "com.vng.pubgmobile", "com.pubg.imobile", "com.rekoo.pubgm"));

    static boolean isGame(String packageName) {
        return packageName != null && PACKAGES.contains(packageName);
    }

    static boolean hasUsageAccess(Context context) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        return appOps != null && appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(), context.getPackageName()) == AppOpsManager.MODE_ALLOWED;
    }

    static final class Observer {
        private final Context context;
        private final UsageStatsManager manager;
        private String mostRecentForeground = "";
        private long lastQueryAt = 0L;

        Observer(Context context) {
            this.context = context.getApplicationContext();
            manager = (UsageStatsManager) this.context.getSystemService(Context.USAGE_STATS_SERVICE);
        }

        boolean gameInForeground() {
            if (manager == null || !hasUsageAccess(context)) return false;
            PowerManager power = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (power != null && !power.isInteractive()) return false;

            final long now = System.currentTimeMillis();
            // The initial history allows a game already foregrounded before we
            // registered to be detected; subsequent checks scan only recent events.
            final long since = lastQueryAt == 0L
                    ? now - 6L * 60L * 60L * 1000L
                    : Math.max(now - 30_000L, lastQueryAt - 2000L);
            lastQueryAt = now;
            UsageEvents events = manager.queryEvents(Math.max(0L, since), now);
            if (events == null) return false;
            UsageEvents.Event event = new UsageEvents.Event();
            long newestTime = -1L;
            String newestPackage = null;
            while (events.hasNextEvent()) {
                events.getNextEvent(event);
                int type = event.getEventType();
                if (type == UsageEvents.Event.ACTIVITY_RESUMED ||
                        type == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    if (event.getTimeStamp() >= newestTime) {
                        newestTime = event.getTimeStamp();
                        newestPackage = event.getPackageName();
                    }
                }
            }
            if (newestPackage != null) mostRecentForeground = newestPackage;
            return isGame(mostRecentForeground);
        }
    }

    private GameRuntime() { }
}
