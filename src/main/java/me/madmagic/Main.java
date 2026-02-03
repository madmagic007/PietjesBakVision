package me.madmagic;

import me.madmagic.webinterface.ServerInstance;

public class Main {

    public static int camIndex = 1;

    public static void main(String[] args) throws Exception {
        ServerInstance.init();

        if (args.length != 0) {
            camIndex = Integer.parseInt(args[0]);
        }
    }
}

