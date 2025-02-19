package com.pistacium.modcheck;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.pistacium.modcheck.mod.ModData;
import com.pistacium.modcheck.mod.version.ModVersion;
import com.pistacium.modcheck.util.ModCheckStatus;
import com.pistacium.modcheck.util.ModCheckUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ModCheck {

    public static final Logger LOGGER = Logger.getLogger("ModCheck");

    public static void setStatus(ModCheckStatus status) {
        FRAME_INSTANCE.getProgressBar().setString(status.getDescription());
    }

    public static final ExecutorService THREAD_EXECUTOR = Executors.newSingleThreadExecutor();

    public static ModCheckFrame FRAME_INSTANCE;

    public static final ArrayList<ModVersion> AVAILABLE_VERSIONS = new ArrayList<>();

    public static final ArrayList<ModData> AVAILABLE_MODS = new ArrayList<>();


    public static void main(String[] args) {
        THREAD_EXECUTOR.submit(() -> {
            try {
                FRAME_INSTANCE = new ModCheckFrame();

                // Get available versions
                setStatus(ModCheckStatus.LOADING_AVAILABLE_VERSIONS);
                JsonElement availableElement = JsonParser.parseString(Objects.requireNonNull(ModCheckUtils.getUrlRequest("https://redlime.github.io/MCSRMods/mod_versions.json")));
                FRAME_INSTANCE.getProgressBar().setValue(5);
                for (JsonElement jsonElement : availableElement.getAsJsonArray()) {
                    AVAILABLE_VERSIONS.add(ModVersion.of(jsonElement.getAsString()));
                }

                // Get mod list
                setStatus(ModCheckStatus.LOADING_MOD_LIST);
                JsonElement modElement = JsonParser.parseString(Objects.requireNonNull(ModCheckUtils.getUrlRequest("https://redlime.github.io/MCSRMods/mods.json")));
                FRAME_INSTANCE.getProgressBar().setValue(10);

                setStatus(ModCheckStatus.LOADING_MOD_RESOURCE);
                int count = 0, maxCount = modElement.getAsJsonArray().size();
                for (JsonElement jsonElement : modElement.getAsJsonArray()) {
                    try {
                        if (Objects.equals(jsonElement.getAsJsonObject().get("type").getAsString(), "fabric_mod")) {
                            FRAME_INSTANCE.getProgressBar().setString("Loading information of "+jsonElement.getAsJsonObject().get("name"));
                            ModData modData = new ModData(jsonElement.getAsJsonObject());
                            AVAILABLE_MODS.add(modData);
                        }
                    } catch (Throwable e) {
                        LOGGER.log(Level.WARNING, "Failed to init " + jsonElement.getAsJsonObject().get("name").getAsString() + "!", e);
                    } finally {
                        FRAME_INSTANCE.getProgressBar().setValue((int) (10 + (((++count * 1f) / maxCount) * 90)));
                    }
                }
                FRAME_INSTANCE.getProgressBar().setValue(100);
                setStatus(ModCheckStatus.IDLE);
                FRAME_INSTANCE.updateVersionList();
            } catch (Throwable e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                int result = JOptionPane.showOptionDialog(null, sw.toString(), "Error exception!",
                        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
                        new String[] { "Copy to Clipboard", "Cancel" }, "Copy to Clipboard");
                if (result == 0) {
                    StringSelection selection = new StringSelection(sw.toString());
                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    clipboard.setContents(selection, selection);
                }

                System.exit(0);
            }
        });
        //System.out.println(new Gson().toJson(ModCheckUtils.getFabricJsonFileInJar(new File("D:/MultiMC/instances/1.16-1/.minecraft/mods/SpeedRunIGT-10.0+1.16.1.jar"))));
    }

}
