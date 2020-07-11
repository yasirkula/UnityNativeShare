package com.yasirkula.unity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by yasirkula on 11.07.2020.
 */

public class NativeShareFragment extends Fragment
{
	@TargetApi( 22 )
	public static class ShareResultBroadcastReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive( Context context, Intent intent )
		{
			if( broadcastReceiver != null )
			{
				context.getApplicationContext().unregisterReceiver( broadcastReceiver );
				broadcastReceiver = null;
			}
			else
				Log.e( "Unity", "ShareResultBroadcastReceiver was null!" );

			if( shareResultReceiver != null )
			{
				ComponentName selectedShareTarget = intent.getParcelableExtra( Intent.EXTRA_CHOSEN_COMPONENT );
				if( selectedShareTarget != null )
				{
					String selectedShareTargetStr = selectedShareTarget.flattenToString();
					Log.d( "Unity", "Shared on app: " + selectedShareTargetStr );

					shareResultReceiver.OnShareCompleted( 1, selectedShareTargetStr ); // 1: Shared
				}
				else
				{
					Log.d( "Unity", "Shared on app: Unknown" );
					shareResultReceiver.OnShareCompleted( 1, "" ); // 1: Shared
				}
			}
			else
				Log.e( "Unity", "NativeShareResultReceiver was null!" );
		}
	}

	private static final String BROADCAST_RECEIVER_FILTER = "com.yasirkula.unity.NATIVESHARE_RESULTRECEIVER";
	private static final int SHARE_RESULT_CODE = 774457;

	public static final String TARGET_PACKAGE_ID = "NS_TARGET_PACKAGE";
	public static final String TARGET_CLASS_ID = "NS_TARGET_CLASS";
	public static final String FILES_ID = "NS_FILES";
	public static final String MIMES_ID = "NS_MIMES";
	public static final String SUBJECT_ID = "NS_SUBJECT";
	public static final String TEXT_ID = "NS_TEXT";
	public static final String TITLE_ID = "NS_TITLE";

	private static NativeShareResultReceiver shareResultReceiver;
	private static ShareResultBroadcastReceiver broadcastReceiver;

	public NativeShareFragment()
	{
		shareResultReceiver = null;
	}

	public NativeShareFragment( final NativeShareResultReceiver shareResultReceiver )
	{
		NativeShareFragment.shareResultReceiver = shareResultReceiver;
	}

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );

		if( shareResultReceiver == null )
			getFragmentManager().beginTransaction().remove( this ).commit();
		else
		{
			String targetPackage = getArguments().getString( NativeShareFragment.TARGET_PACKAGE_ID );
			String targetClass = getArguments().getString( NativeShareFragment.TARGET_CLASS_ID );
			String subject = getArguments().getString( NativeShareFragment.SUBJECT_ID );
			String text = getArguments().getString( NativeShareFragment.TEXT_ID );
			String title = getArguments().getString( NativeShareFragment.TITLE_ID );
			ArrayList<String> files = getArguments().getStringArrayList( NativeShareFragment.FILES_ID );
			ArrayList<String> mimes = getArguments().getStringArrayList( NativeShareFragment.MIMES_ID );

			Intent intent = new Intent();

			if( subject.length() > 0 )
				intent.putExtra( Intent.EXTRA_SUBJECT, subject );

			if( text.length() > 0 )
				intent.putExtra( Intent.EXTRA_TEXT, text );

			String mime;
			if( files.size() > 0 )
			{
				String mimeType = null;
				String mimeSubtype = null;
				for( int i = 0; i < files.size(); i++ )
				{
					String thisMime;
					if( mimes.get( i ).length() > 0 )
						thisMime = mimes.get( i );
					else
					{
						int extensionStart = files.get( i ).lastIndexOf( '.' );
						if( extensionStart < 0 || extensionStart == files.size() - 1 )
						{
							mimeType = mimeSubtype = "*";
							break;
						}

						// Credit: https://stackoverflow.com/a/31691791/2373034
						thisMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension( files.get( i ).substring( extensionStart + 1 ).toLowerCase( Locale.ENGLISH ) );
					}

					if( thisMime == null || thisMime.length() == 0 )
					{
						mimeType = mimeSubtype = "*";
						break;
					}

					int mimeDivider = thisMime.indexOf( '/' );
					if( mimeDivider <= 0 || mimeDivider == thisMime.length() - 1 )
					{
						mimeType = mimeSubtype = "*";
						break;
					}

					String thisMimeType = thisMime.substring( 0, mimeDivider );
					String thisMimeSubtype = thisMime.substring( mimeDivider + 1 );

					if( mimeType == null )
						mimeType = thisMimeType;
					else if( !mimeType.equals( thisMimeType ) )
					{
						mimeType = mimeSubtype = "*";
						break;
					}

					if( mimeSubtype == null )
						mimeSubtype = thisMimeSubtype;
					else if( !mimeSubtype.equals( thisMimeSubtype ) )
						mimeSubtype = "*";
				}

				mime = mimeType + "/" + mimeSubtype;

				if( files.size() == 1 )
				{
					intent.setAction( Intent.ACTION_SEND );

					Uri contentUri = NativeShareContentProvider.getUriForFile( getActivity(), NativeShare.authority, new File( files.get( 0 ) ) );
					intent.putExtra( Intent.EXTRA_STREAM, contentUri );
				}
				else
				{
					// Credit: https://stackoverflow.com/a/27514002/2373034
					intent.setAction( Intent.ACTION_SEND_MULTIPLE );
					ArrayList<Uri> uris = new ArrayList<Uri>( files.size() );
					for( int i = 0; i < files.size(); i++ )
					{
						Uri contentUri = NativeShareContentProvider.getUriForFile( getActivity(), NativeShare.authority, new File( files.get( i ) ) );
						uris.add( contentUri );
					}

					intent.putParcelableArrayListExtra( Intent.EXTRA_STREAM, uris );
				}
			}
			else
			{
				mime = "text/plain";
				intent.setAction( Intent.ACTION_SEND );
			}

			if( title.length() > 0 )
				intent.putExtra( Intent.EXTRA_TITLE, title );

			intent.setType( mime );
			intent.setFlags( Intent.FLAG_GRANT_READ_URI_PERMISSION );

			if( targetPackage.length() > 0 )
			{
				intent.setPackage( targetPackage );

				if( targetClass.length() > 0 )
					intent.setClassName( targetPackage, targetClass );
			}

			if( Build.VERSION.SDK_INT < 22 )
				startActivityForResult( Intent.createChooser( intent, title ), SHARE_RESULT_CODE );
			else
			{
				Intent receiverIntent = new Intent( BROADCAST_RECEIVER_FILTER );
				PendingIntent pendingIntent = PendingIntent.getBroadcast( getActivity(), 0, receiverIntent, PendingIntent.FLAG_UPDATE_CURRENT );

				if( broadcastReceiver == null )
				{
					broadcastReceiver = new ShareResultBroadcastReceiver();
					getActivity().getApplication().registerReceiver( broadcastReceiver, new IntentFilter( BROADCAST_RECEIVER_FILTER ) );
				}

				startActivityForResult( Intent.createChooser( intent, title, pendingIntent.getIntentSender() ), SHARE_RESULT_CODE );
			}
		}
	}

	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent data )
	{
		if( requestCode != SHARE_RESULT_CODE )
			return;

		if( shareResultReceiver != null )
		{
			Log.d( "Unity", "Reported share result (may not be correct): " + ( resultCode == Activity.RESULT_OK ) );

			if( resultCode == Activity.RESULT_OK )
				shareResultReceiver.OnShareCompleted( 1, "" ); // 1: Shared
			else
			{
				if( Build.VERSION.SDK_INT < 22 )
				{
					// On older Android versions, unfortunately we can't determine whether or not user has picked an app
					// from the share sheet
					shareResultReceiver.OnShareCompleted( 0, "" ); // 0: Unknown
				}
				else
				{
					// On newer Android versions, it is safe to send NotShared result since for a successful share,
					// ShareResultBroadcastReceiver will override the result
					shareResultReceiver.OnShareCompleted( 2, "" ); // 2: NotShared
				}
			}
		}
		else
			Log.e( "Unity", "NativeShareResultReceiver was null!" );

		getFragmentManager().beginTransaction().remove( this ).commit();
	}
}