= Native Share for Android & iOS =

Online documentation & example code available at: https://github.com/yasirkula/UnityNativeShare
E-mail: yasirkula@gmail.com

1. ABOUT
This plugin helps you natively share files (images, videos, documents, etc.) and/or plain text on Android & iOS. A ContentProvider is used to share the media on Android.

2. HOW TO
for Android: using a ContentProvider requires a small modification in AndroidManifest. If your project does not have an AndroidManifest.xml file located at Assets/Plugins/Android, you should copy Unity's default AndroidManifest.xml from C:\Program Files\Unity\Editor\Data\PlaybackEngines\AndroidPlayer (it might be located in a subfolder, like 'Apk') to Assets/Plugins/Android. Inside the <application>...</application> tag of your AndroidManifest, insert the following code snippet:

<provider
  android:name="com.yasirkula.unity.UnitySSContentProvider"
  android:authorities="MY_UNIQUE_AUTHORITY"
  android:exported="false"
  android:grantUriPermissions="true" />

Here, you should change MY_UNIQUE_AUTHORITY with a unique string. That is important because two apps with the same android:authorities string in their <provider> tag can't be installed on the same device. Just make it something unique, like your bundle identifier, if you like.

NOTE: if you are also using the NativeCamera plugin, make sure that each plugin's provider has a different android:authorities string.

for iOS: there are two ways to set up the plugin on iOS:

a. Automated Setup for iOS
- change the value of PHOTO_LIBRARY_USAGE_DESCRIPTION in Plugins/NativeShare/Editor/NSPostProcessBuild.cs (optional)

b. Manual Setup for iOS
- set the value of ENABLED to false in NSPostProcessBuild.cs
- build your project
- enter a Photo Library Usage Description to Info.plist in Xcode (in case user decides to save the shared media to Photos)
- also enter a Photo Library Additions Usage Description to Info.plist in Xcode, if exists

3. FAQ
- I can't share image with text on X app
It is just not possible to share an image/file with text/subject on some apps (e.g. Facebook), they intentionally omit either the image or the text from the shared content. These apps require you to use their own SDKs for complex share actions. For best compatibility, I'd recommend you to share either only image or only text.

- Can't share, it says "Can't file ContentProvider, share not possible!" in Logcat
Make sure that you've added the provider to the AndroidManifest.xml located exactly at Assets/Plugins/Android and verify that it is inserted in-between the <application>...</application> tags.

- Can't share, it says "java.lang.ClassNotFoundException: com.yasirkula.unity.NativeShare" in Logcat
If your project uses ProGuard, try adding the following line to ProGuard filters: -keep class com.yasirkula.unity.* { *; }

- My app crashes at startup after importing NativeShare to my project
Make sure that you didn't touch the provider's android:name value, it must stay as is. You only need to change the android:authorities string.

4. SCRIPTING API
Simply create a new NativeShare object and customize it by chaining the following functions as you like:

- SetSubject( string subject ): sets the subject (primarily used in e-mail applications)
- SetText( string text ): sets the shared text. Note that the Facebook app will omit text, if exists
- AddFile( string filePath, string mime = null ): adds the file at path to the share action. You can add multiple files of different types. The MIME of the file is automatically determined if left null; however, if the file doesn't have an extension and/or you already know the MIME of the file, you can enter the MIME manually. MIME has no effect on iOS
- SetTitle( string title ): sets the title of the share dialog on Android platform. Has no effect on iOS
- SetTarget( string androidPackageName, string androidClassName = null ): shares content on a specific application on Android platform. If androidClassName is left null, list of activities in the share dialog will be narrowed down to the activities in the specified androidPackageName that can handle this share action (if there is only one such activity, it will be launched directly). Note that androidClassName, if provided, must be the full name of the activity (with its package). This function has no effect on iOS

Finally, calling the Share() function of the NativeShare object will do the trick!

5. KNOWN LIMITATIONS
- Gif files are shared as static images on iOS