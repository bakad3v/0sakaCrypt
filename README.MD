# 0sakaCrypt
[Readme на русском](./README_RU.md)

Follow me on [telegram](https://t.me/bakad3v) and [twitter](https://x.com/bakad3v) to keep up with updates.

## #Keep android open
You can read my statement on the upcoming sideloading ban and learn some ways to circumwent it [here](https://medium.com/@bakad3v/android-sideloading-matters-6d0e25684960).

## Description 
0sakaCrypt is a virtual disk encryption software for Android which allows you to store your files in an encrypted container. It is a fork of [NeverCrypt](https://github.com/cobol56/nevercrypt), and NeverCrypt is a fork of EDS Lite, the open source edition of EDS. Visit the [EDS web site](https://sovworks.com/eds/) to learn about EDS features and capabilities.

## Features and limitation
It's better to use this application with [GrapheneOS](https://grapheneos.org/)!

Unlike NeverCrypt and EDS Lite, 0sakaCrypt focuses on working with VeraCrypt hidden volumes. It supports creating and opening hidden volumes, as well as protecting hidden volumes from accidental damage when data is written to the outer volume.

Like NeverCrypt, 0sakaCrypt uses the Android Storage Access Framework to provide external applications with access to files stored inside encrypted containers.

**This is an alpha version, so please don't trust it with important files for now!**

The application's security model and limitations are similar to [those of VeraCrypt](https://veracrypt.io/en/Security%20Model.html).

If you require plausible deniability, read VeraCrypt's [Security Requirements and Precautions Pertaining to Hidden Volumes](https://veracrypt.io/en/Security%20Requirements%20for%20Hidden%20Volumes.html) and follow the recommendations provided there.

Unfortunately, not all of these recommendations can be followed on Android. The application does not support partition-hosted volumes, container files are normally stored on a journaling ext4 filesystem, and Android devices commonly use flash storage with wear leveling. However, exploiting these Android-specific limitations is technically difficult and does not guarantee that an adversary will be able to detect the existence of a hidden volume.

A more serious limitation is that Android devices generally cannot boot a separate live operating system for securely working with sensitive files. However, the Usage recommendations section describes one way to minimize traces left by files from hidden volumes.

The most important fundamental limitation of plausible deniability is that your decoy data must be believable. An adversary may simply refuse to believe that the outer volume is your only encrypted volume and continue demanding the password for a hidden volume.

Make sure that the data in the decoy volume appears valuable enough to justify encryption. Avoid making the hidden volume unnecessarily large, and be prepared to explain why you chose to store the decoy data in an encrypted container.

## Usage recommendations
The application's interface and capabilities are similar to those of VeraCrypt. You can visit the [VeraCrypt website](https://veracrypt.io) to find out more about operations with VeraCrypt containers.

When you open a hidden volume and edit its files using external applications, those applications may store metadata that reveals the existence or contents of files from the hidden volume. To reduce this risk, I recommend opening encrypted containers inside temporary work profiles. Work profiles are essentially separate Android users whose data is protected with a separate encryption key. After a work profile is deleted, its encrypted data can generally be made inaccessible quickly because the corresponding encryption keys are destroyed.

You can use the following procedure:
1. Create the temporary working profile using [Shelter](https://f-droid.org/packages/net.typeblog.shelter/) or [Island](https://github.com/oasisfeng/island/releases).
2. Install 0sakaCrypt in the working profile.
3. Open the hidden volume from your main profile in the working profile.
4. Install all necessary applications in the working profile
5. After end of work with container, close it and delete working profile

Work-profile management is planned to be integrated directly into 0sakaCrypt.

To protect an unlocked encrypted volume in a situation where you may be forced to provide your device password, you can use [Android AntiForensic Tools](https://github.com/bakad3v/Android-AntiForensic-Tools). It can be configured to delete itself and the temporary work profile used to access the container. Unfortunately, it needs root rights to work properly.

Some Android AntiForensic Tools capabilities are planned to be integrated into 0sakaCrypt and will work without root access.

When file is edited, application creates it's backup copy. **Make sure there's enough space for the copy of edited file in container, otherwise all changes can be lost!**

## Acknowledgements
[sovworks](https://sovworks.com/eds/), the developers of the original EDS lite

[IGEL Tech GmbH](https://www.igel-tech.com/), the developers of the Nevercrypt

The developers of the [VeraCrypt](https://veracrypt.io/)

## Donations
You can support me using crypto.

XMR: 88Z5fsVK6FP4oVNjo2BrHydAEa5Y1gTPi5d7BN68sjVDZ9dTn8wPb89WmUxrxf3T37bRGSR5dekkU9aQ7j8ErWcBJ2GZojC

LTC: ltc1qtfhun6yzt0qlpa9d6s8gr2yd70sg0kx755u3se

BTC: bc1qn9a9eyy2vgrz8yvhk0654d63dt56pxhpn6q4xl

BTC lightning: dualwhorl66@walletofsatoshi.com
