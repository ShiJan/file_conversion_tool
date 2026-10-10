package dev.wechatarchive;

import dev.wechatarchive.ui.OrganizerApp;
import javafx.application.Application;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        if (args.length == 1 && "--self-test".equals(args[0])) {
            PortableSelfTest.run();
            return;
        }
        Application.launch(OrganizerApp.class, args);
    }
}
