using UnityEngine;
using System.IO;
using System.Collections.Generic;

#pragma warning disable 0414
public class NativeShare
{
#if !UNITY_EDITOR && UNITY_ANDROID
	private static AndroidJavaClass m_ajc = null;
	private static AndroidJavaClass AJC
	{
		get
		{
			if( m_ajc == null )
				m_ajc = new AndroidJavaClass( "com.yasirkula.unity.NativeShare" );

			return m_ajc;
		}
	}

	private static AndroidJavaObject m_context = null;
	private static AndroidJavaObject Context
	{
		get
		{
			if( m_context == null )
			{
				using( AndroidJavaObject unityClass = new AndroidJavaClass( "com.unity3d.player.UnityPlayer" ) )
				{
					m_context = unityClass.GetStatic<AndroidJavaObject>( "currentActivity" );
				}
			}

			return m_context;
		}
	}
#elif !UNITY_EDITOR && UNITY_IOS
	[System.Runtime.InteropServices.DllImport( "__Internal" )]
	private static extern void _Share( string[] files, int filesCount, string subject, string text );
#endif

	private string subject;
	private string text;
	private string title;
	private string authority;

	private List<string> files;
	private List<string> mimes;

	public NativeShare()
	{
		subject = string.Empty;
		text = string.Empty;
		title = string.Empty;
		authority = null;

		files = new List<string>( 0 );
		mimes = new List<string>( 0 );
	}

	public NativeShare SetSubject( string subject )
	{
		if( subject != null )
			this.subject = subject;

		return this;
	}

	public NativeShare SetText( string text )
	{
		if( text != null )
			this.text = text;

		return this;
	}

	public NativeShare SetTitle( string title )
	{
		if( title != null )
			this.title = title;

		return this;
	}

	public NativeShare SetAuthority( string authority )
	{
		if( authority != null )
			this.authority = authority;

		return this;
	}

	public NativeShare AddFile( string filePath, string mime = null )
	{
		if( !string.IsNullOrEmpty( filePath ) && File.Exists( filePath ) )
		{
			files.Add( filePath );
			mimes.Add( mime ?? string.Empty );
		}
		else
			Debug.LogError( "File does not exist at path or permission denied: " + filePath );

		return this;
	}

	public void Share()
	{
		if( files.Count == 0 && subject.Length == 0 && text.Length == 0 )
		{
			Debug.LogWarning( "Share Error: attempting to share nothing!" );
			return;
		}

#if UNITY_EDITOR
		Debug.Log( "Shared!" );
#elif UNITY_ANDROID
		if( string.IsNullOrEmpty( authority ) )
			throw new System.ArgumentException( "Share Error: 'authority' is null or empty!" );
		
		AJC.CallStatic( "Share", Context, files.ToArray(), mimes.ToArray(), subject, text, title, authority );
#elif UNITY_IOS
		_Share( files.ToArray(), files.Count, subject, text );
#else
		Debug.Log( "No sharing set up for this platform." );
#endif
	}
}
#pragma warning restore 0414
