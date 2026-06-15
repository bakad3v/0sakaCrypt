package com.igeltech.nevercrypt.veracrypt

class HiddenVolumeLayout: VolumeLayout() {

    override fun getHeaderOffset(): Long {
        return (HEADER_SIZE).toLong()
    }

}