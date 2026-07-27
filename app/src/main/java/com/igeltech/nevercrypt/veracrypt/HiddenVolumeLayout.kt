package com.igeltech.nevercrypt.veracrypt

class HiddenVolumeLayout: VolumeLayout() {

    /**
     * Sets host container geometry and positions the hidden payload at the end of the host data area.
     */
    fun setContainerSize(containerSize: Long, hiddenVolumeSize: Long) {
        _inputSize = containerSize
        _volumeSize = HiddenVolumeUtils.alignDown(hiddenVolumeSize, SECTOR_SIZE.toLong())
        _encryptedAreaStart = containerSize - 2L * HEADER_SIZE - _volumeSize
        require(_volumeSize > 0) { "Hidden volume size is too small" }
        require(_encryptedAreaStart >= 2L * HEADER_SIZE) { "Hidden volume does not fit into the host volume" }
    }

    /**
     * Prepares the payload encryption engine without writing hidden headers yet.
     */
    fun preparePayloadEncryptionEngine() {
        prepareEncryptionEngineForPayload()
    }

    /**
     * Returns the primary hidden header offset inside a standard VeraCrypt host.
     */
    override fun getHeaderOffset(): Long {
        return (HEADER_SIZE).toLong()
    }

    /**
     * Returns the backup hidden header offset at the end of the host container.
     */
    override fun getBackupHeaderOffset(): Long {
        return _inputSize - HEADER_SIZE
    }

    /**
     * Encodes the hidden payload size into the hidden-volume-size header field.
     */
    override fun calcHiddenVolumeSize(volumeSize: Long): Long {
        return volumeSize
    }

}
