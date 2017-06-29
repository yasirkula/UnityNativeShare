# UnityNativeShare
This plugin helps you natively share your images and/or videos on Android & iOS. A **ContentProvider** is used to share the media on Android. You can set the plugin up in a few easy steps:

- Import **NativeShare.unitypackage** to your project
- *for iOS*: enter a **Photo Library Usage Description** in Xcode

![PhotoLibraryUsageDescription](iOSPhotoLibraryPermission.png)

- *for Android*: using a ContentProvider requires a small modification in AndroidManifest. If your project does not have an **AndroidManifest.xml** file located at **Assets/Plugins/Android**, you should copy Unity's default AndroidManifest.xml from *C:\Program Files\Unity\Editor\Data\PlaybackEngines\AndroidPlayer* (it might be located in a subfolder, like '*Apk*') to *Assets/Plugins/Android* ([credit](http://answers.unity3d.com/questions/536095/how-to-write-an-androidmanifestxml-combining-diffe.html)).

- Inside the `<application>...</application>` tag of your AndroidManifest, insert the following code snippet:

```xml
<provider
  android:name="com.yasirkula.unity.UnitySSContentProvider"
  android:authorities="MY_UNIQUE_AUTHORITY"
  android:exported="false"
  android:grantUriPermissions="true" />
```

- Here, you should change **MY_UNIQUE_AUTHORITY** with a **unique string**. That is important because two apps with the same **android:authorities** string in their `<provider>` tag can't be installed on the same device. Just make it something unique, like your bundle identifier, if you like.

## How To
Simply call `NativeShare.Share( string mediaPath, bool isPicture, string authority = null )` to share an image or video. The file must be stored on the disk before calling this function (though, you can store it in a temporary location, if you prefer, such as **Application.temporaryCachePath**). 
- **mediaPath:** the path of your file
- **isImage:** should be true for images and false for videos
- **authority:** should be the same string that you replaced *MY_UNIQUE_AUTHORITY* with (this parameter has no effect on iOS)

## Example Code
The following code captures the screenshot of the game whenever you tap the screen, saves it in a temporary path and then shares the image:

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

	NativeShare.Share( filePath, true, "nativeshare.test" );
}
```
