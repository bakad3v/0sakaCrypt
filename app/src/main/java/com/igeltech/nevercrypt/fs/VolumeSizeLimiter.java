package com.igeltech.nevercrypt.fs;

import java.io.IOException;

/**
 * File systems implementing this interface can expose a smaller writable area
 * than the underlying encrypted container while leaving the container itself open.
 */
public interface VolumeSizeLimiter
{
    /**
     * Caps allocations and writes to the given logical volume size.
     */
    void setVolumeSizeLimit(long volumeSizeLimit) throws IOException;
}
