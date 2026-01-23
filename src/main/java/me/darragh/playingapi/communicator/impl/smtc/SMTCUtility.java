package me.darragh.playingapi.communicator.impl.smtc;

import lombok.experimental.UtilityClass;

import java.io.File;

@UtilityClass
public final class SMTCUtility {
    public static File extractDLL(String dllName) {
        try {
            var inputStream = SMTCUtility.class.getResourceAsStream("/" + dllName);
            if (inputStream == null) {
                throw new IllegalStateException("DLL resource not found: " + dllName);
            }

            File tempFile = File.createTempFile(dllName, "");
            tempFile.deleteOnExit();

            try (var outputStream = java.nio.file.Files.newOutputStream(tempFile.toPath())) {
                inputStream.transferTo(outputStream);
            }

            return tempFile;
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract DLL: " + dllName, e);
        }
    }
}
