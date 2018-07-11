# Unity Native Share Plugin

**Available on Asset Store:** https://www.assetstore.unity3d.com/en/#!/content/112731

**Forum Thread:** https://forum.unity.com/threads/native-share-for-android-ios-open-source.519865/

This plugin helps you natively share files (images, videos, documents, etc.) and/or plain text on Android & iOS. A **ContentProvider** is used to share the media on Android. 

After importing **NativeShare.unitypackage** to your project, only a few steps are required to set up the plugin:

### Android Setup

- using a ContentProvider requires a small modification in AndroidManifest. If your project does not have an **AndroidManifest.xml** file located at **Assets/Plugins/Android**, you should copy Unity's default AndroidManifest.xml from *C:\Program Files\Unity\Editor\Data\PlaybackEngines\AndroidPlayer* (it might be located in a subfolder, like '*Apk*') to *Assets/Plugins/Android* ([credit](http://answers.unity3d.com/questions/536095/how-to-write-an-androidmanifestxml-combining-diffe.html))
- inside the `<application>...</application>` tag of your AndroidManifest, insert the following code snippet:

```xml
<provider
  android:name="com.yasirkula.unity.UnitySSContentProvider"
  android:authorities="MY_UNIQUE_AUTHORITY"
  android:exported="false"
  android:grantUriPermissions="true" />
```

Here, you should change **MY_UNIQUE_AUTHORITY** with a **unique string**. That is important because two apps with the same **android:authorities** string in their `<provider>` tag can't be installed on the same device. Just make it something unique, like your bundle identifier, if you like.

To verify this step, you can check the contents of *Temp/StagingArea/AndroidManifest.xml* to see if the *<provider ... />* is still there **after** building your project to Android. 

### iOS Setup

There are two ways to set up the plugin on iOS:

#### a. Automated Setup for iOS

- (optional) change the value of **PHOTO_LIBRARY_USAGE_DESCRIPTION** in *Plugins/NativeShare/Editor/NSPostProcessBuild.cs*

#### b. Manual Setup for iOS

- set the value of **ENABLED** to *false* in *NSPostProcessBuild.cs*
- build your project
- enter a **Photo Library Usage Description** in Xcode (in case user decides to save the shared media to Photos)

![PhotoLibraryUsageDescription](iOSPhotoLibraryPermission.png)

- also enter a **Photo Library Additions Usage Description**, if exists (see: https://github.com/yasirkula/UnityNativeGallery/issues/3)

## Upgrading From Previous Versions
Delete *Plugins/NativeShare.cs*, *Plugins/Android/NativeShare.jar* and *Plugins/iOS/NativeShare.mm* before upgrading the plugin.

## How To
Simply create a new **NativeShare** object and customize it by chaining the following functions as you like (see example code):

- `SetSubject( string subject )`: sets the subject (primarily used in e-mail applications)
- `SetText( string text )`: sets the shared text. Note that the Facebook app will omit text, if exists (see [this topic](https://stackoverflow.com/a/35102802/2373034))
- `AddFile( string filePath, string mime = null )`: adds the file at path to the share action. You can add multiple files of different types. The MIME of the file is automatically determined if left null; however, if the file doesn't have an extension and/or you already know the MIME of the file, you can enter the MIME manually. MIME has no effect on iOS
- `SetTitle( string title )`: sets the title of the share dialog on Android platform. Has no effect on iOS
- `SetTarget( string androidPackageName, string androidClassName = null )`: shares content on a specific application on Android platform. If *androidClassName* is left null, list of activities in the share dialog will be narrowed down to the activities in the specified *androidPackageName* that can handle this share action (if there is only one such activity, it will be launched directly). Note that androidClassName, if provided, must be the full name of the activity (with its package). This function has no effect on iOS

Finally, calling the **Share()** function of the NativeShare object will do the trick!

## Utility Functions
- `bool NativeShare.TargetExists( string androidPackageName, string androidClassName = null )`: returns whether the application with the specified package/class name exists on the Android device. If *androidClassName* is left null, only the package name is queried. This function always returns true on iOS
- `bool FindTarget( out string androidPackageName, out string androidClassName, string packageNameRegex, string classNameRegex = null )`: finds the package/class name of an installed application on the Android device using regular expressions. Returns true if a matching package/class name is found successfully. Can be useful when you want to use the *SetTarget* function but don't know the exact package/class name of the target activity. If *classNameRegex* is left null, the first activity in the matching package is returned. This function always returns false on iOS

## Example Code
The following code captures the screenshot of the game whenever you tap the screen, saves it in a temporary path and then shares it:

```csharp
void Update()
{
	if( Input.GetMouseButtonDown( 0 ) )
		StartCoroutine( TakeSSAndShare() );
}
	
private IEnumerator TakeSSAndShare()
{
	yield return new WaitForEndOfFrame();

	Texture2D ss = new Texture2D( Screen.width, Screen.height, TextureFormat.RGB24, false );
	ss.ReadPixels( new Rect( 0, 0, Screen.width, Screen.height ), 0, 0 );
	ss.Apply();

	string filePath = Path.Combine( Application.temporaryCachePath, "shared img.png" );
	File.WriteAllBytes( filePath, ss.EncodeToPNG() );
	
	// To avoid memory leaks
	Destroy( ss );

	new NativeShare().AddFile( filePath ).SetSubject( "Subject goes here" ).SetText( "Hello world!" ).Share();

	// Share on WhatsApp only, if installed (Android only)
	//if( NativeShare.TargetExists( "com.whatsapp" ) )
	//	new NativeShare().AddFile( filePath ).SetText( "Hello world!" ).SetTarget( "com.whatsapp" ).Share();
}
```

## Known Limitations
- Gif files are shared as static images on iOS (to learn more, please see this issue: https://github.com/yasirkula/UnityNativeShare/issues/22)
