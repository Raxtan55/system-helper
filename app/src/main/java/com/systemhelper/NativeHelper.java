package com.systemhelper;

public class NativeHelper {

    public static class PlayerInfo {
        public String name;
        public String colorName;
        public int role;
        public boolean isDead;
        public boolean isImpostor;
    }

    public static boolean init(long baseAddr) {
        return baseAddr != 0;
    }

    public static boolean isGameRunning() {
        return false;
    }

    public static PlayerInfo[] getPlayerList() {
        return null;
    }

    public static long getModuleBase() {
        return 0;
    }
}
