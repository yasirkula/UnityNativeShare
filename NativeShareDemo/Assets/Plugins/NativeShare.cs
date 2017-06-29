using UnityEngine;
using System.Collections;
using System.Runtime.InteropServices;
using System.IO;

/*
 * Credit: https://github.com/ChrisMaire/unity-native-sharing
 */

public static class NativeShare
{
#if !UNITY_EDITOR && UNITY_IOS
    [System.Runtime.InteropServices.DllImport( "__Internal" )]
    private static extern void _ShareImage( string path );

    [System.Runtime.InteropServices.DllImport( "__Internal" )]
    private static extern void _ShareVideo( string path );
#endif

    public static void Share( string mediaPath, bool isPicture, string authority = null )
    {
		if( !File.Exists( mediaPath ) )
			throw new FileNotFoundException( "File not found at " + mediaPath );

#if UNITY_EDITOR
#elif UNITY_ANDROID
		if( authority == null || authority.Length == 0 )
			throw new System.ArgumentException( "Parameter 'authority' is null or empty!" );

        using( AndroidJavaClass unityClass = new AndroidJavaClass( "com.unity3d.player.UnityPlayer" ) )
		using( AndroidJavaObject context = unityClass.GetStatic<AndroidJavaObject>( "currentActivity" ) )
		using( AndroidJavaClass nativeShare = new AndroidJavaClass( "com.yasirkula.unity.NativeShare" ) )
        {
            nativeShare.CallStatic( "MediaShareFile", context, mediaPath, authority, isPicture );
        }
		
		// This method (not using a ContentProvider) does not work for LinkedIn and Google+ for some devices (for some reason?)
		// thus, it is replaced with a method that uses ContentProvider
		/*AndroidJavaClass intentClass = new AndroidJavaClass("android.content.Intent");
		AndroidJavaObject intentObject = new AndroidJavaObject("android.content.Intent");

		intentObject.Call<AndroidJavaObject>("setAction", intentClass.GetStatic<string>("ACTION_SEND"));
		AndroidJavaClass uriClass = new AndroidJavaClass("android.net.Uri");
		AndroidJavaObject uriObject = uriClass.CallStatic<AndroidJavaObject>("parse", "file://" + mediaPath);
		intentObject.Call<AndroidJavaObject>("putExtra", intentClass.GetStatic<string>("EXTRA_STREAM"), uriObject);

        if( isPicture )
		    intentObject.Call<AndroidJavaObject>("setType", "image/png");
        else
            intentObject.Call<AndroidJavaObject>("setType", "video/mp4");

		intentObject.Call<AndroidJavaObject>("setFlags", intentClass.GetStatic<int>("FLAG_GRANT_READ_URI_PERMISSION"));

		AndroidJavaClass unity = new AndroidJavaClass("com.unity3d.player.UnityPlayer");
		AndroidJavaObject currentActivity = unity.GetStatic<AndroidJavaObject>("currentActivity");

		AndroidJavaObject jChooser = intentClass.CallStatic<AndroidJavaObject>("createChooser", intentObject, "");
		currentActivity.Call("startActivity", jChooser);*/
#elif UNITY_IOS
        if( isPicture )
		    _ShareImage( mediaPath );
        else
            _ShareVideo( mediaPath );
#else
		Debug.Log( "No sharing set up for this platform." );
#endif
	}
}
