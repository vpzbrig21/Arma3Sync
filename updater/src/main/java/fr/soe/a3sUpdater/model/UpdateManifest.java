package fr.soe.a3sUpdater.model;

import java.net.URI;

/** Normalized update metadata independent of the wire format. */
public record UpdateManifest(
        String version,
        String fileName,
        URI downloadUri,
        String sha256,
        long size,
        Format format,
        String source) {

    public enum Format { JSON, GITHUB, XML }

    public boolean hasSha256() {
        return sha256 != null && !sha256.isBlank();
    }
}
