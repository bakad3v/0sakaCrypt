-dontobfuscate
-optimizations !code/allocation/variable,!code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*

-keep public class com.igeltech.nevercrypt.android.CryptoApplication { public *; }

-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}

-keepnames class * implements java.io.Serializable

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keep public class com.igeltech.nevercrypt.fs.FileSystem
-keep public class com.igeltech.nevercrypt.fs.Path
-keep public class com.igeltech.nevercrypt.fs.RandomAccessIO
-keep interface com.igeltech.nevercrypt.settings.Settings

-keep public class com.igeltech.nevercrypt.android.views.GestureImageView$NavigListener
-keep public class com.igeltech.nevercrypt.android.views.GestureImageView$OptimImageRequiredListener

-keepclassmembers class * implements java.io.Externalizable {
    public void readExternal(java.io.ObjectInput);
    public void writeExternal(java.io.ObjectOutput);
}
-dontwarn javax.annotation.CheckReturnValue
-dontwarn javax.annotation.Nonnull
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.ParametersAreNonnullByDefault
-keep class com.igeltech.nevercrypt.fs.exfat.ExFat { *; }
-keep class com.igeltech.nevercrypt.fs.util.FileStat { *; }
-keep class com.igeltech.nevercrypt.fs.util.Util {
    public static int pread(com.igeltech.nevercrypt.fs.RandomAccessIO, byte[], int, int, long);
    public static int pwrite(com.igeltech.nevercrypt.fs.RandomAccessIO, byte[], int, int, long);
}
-keep interface com.igeltech.nevercrypt.fs.RandomAccessIO { *; }
-keep class com.igeltech.nevercrypt.exceptions.NativeError 