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
	[DllImport( "__Internal" )]
	private static extern void _ShareText( string subject, string message );

	[DllImport( "__Internal" )]
	private static extern void _ShareImage( string path, string subject, string message );

	[DllImport( "__Internal" )]
	private static extern void _ShareVideo( string path, string subject, string message );
#endif

	public static void Share( string mediaPath, string subject, string message, bool isMediaImage, string authority = null )
	{
		if( mediaPath == null )
			mediaPath = string.Empty;

		if( subject == null )
			subject = string.Empty;

		if( message == null )
			message = string.Empty;

		if( mediaPath.Length > 0 && !File.Exists( mediaPath ) )
			throw new FileNotFoundException( "File not found at " + mediaPath );
		
#if UNITY_EDITOR
#elif UNITY_ANDROID
		if( authority == null || authority.Length == 0 )
			throw new System.ArgumentException( "Parameter 'authority' is null or empty!" );

		using( AndroidJavaClass unityClass = new AndroidJavaClass( "com.unity3d.player.UnityPlayer" ) )
		using( AndroidJavaObject context = unityClass.GetStatic<AndroidJavaObject>( "currentActivity" ) )
		using( AndroidJavaClass nativeShare = new AndroidJavaClass( "com.yasirkula.unity.NativeShare" ) )
		{
			nativeShare.CallStatic( "Share", context, mediaPath, subject, message, authority, isMediaImage );
		}
#elif UNITY_IOS
		if( mediaPath.Length == 0 )
			_ShareText( subject, message );
		else if( isMediaImage )
		    _ShareImage( mediaPath, subject, message );
		else
			_ShareVideo( mediaPath, subject, message );
#else
		Debug.Log( "No sharing set up for this platform." );
#endif
	}
}
