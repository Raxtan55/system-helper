package com.systemhelper;

public class NativeHelper {

    static {
        System.loadLibrary("helper");
    }

    public static class PlayerInfo {
        public String name;
        public String colorName;
        public int role;
        public boolean isDead;
        public boolean isImpostor;
    }

    public static native boolean init(long baseAddr);
    public static native boolean isGameRunning();
    public static native PlayerInfo[] getPlayerList();
    public static native long getModuleBase();
}
