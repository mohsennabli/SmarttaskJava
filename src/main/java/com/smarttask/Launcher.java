package com.smarttask;

import com.smarttask.util.DotEnvLoader;

public class Launcher {
    public static void main(String[] args) {
        DotEnvLoader.loadSmartTaskConfig();
        MainApp.main(args);
    }
}

