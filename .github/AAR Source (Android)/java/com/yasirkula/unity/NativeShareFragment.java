package com.yasirkula.unity;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by yasirkula on 11.07.2020.
 */

// Displays standard ACTION_SEND share sheet and waits for its result via onActivityResult and NativeShareBroadcastListener
public class NativeShareFragment extends Fragment
{
	private static final int SHARE_RESULT_CODE = 774457;

	public static final String TARGET_PACKAGE_ID = "NS_TARGET_PACKAGE";
	public static final String TARGET_CLASS_ID = "NS_TARGET_CLASS";
	public static final String FILES_ID = "NS_FILES";
	public static final String MIMES_ID = "NS_MIMES";
	public static final String SUBJECT_ID = "NS_SUBJECT";
	public static final String TEXT_ID = "NS_TEXT";
	public static final String TITLE_ID = "NS_TITLE";

	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );

		if( NativeShare.shareResultReceiver == null )
			getFragmentManager().beginTransaction().remove( this ).commit();
		else
		{
			final Intent shareIntent = NativeShare.CreateIntentFromBundle( getActivity(), getArguments() );
			final String title = getArguments().getString( NativeShareFragment.TITLE_ID );

			if( Build.VERSION.SDK_INT < 22 )
				startActivityForResult( Intent.createChooser( shareIntent, title ), SHARE_RESULT_CODE );
			else
				startActivityForResult( Intent.createChooser( shareIntent, title, NativeShareBroadcastListener.Initialize( getActivity() ) ), SHARE_RESULT_CODE );
		}
	}

	@Override
	public void onActivityResult( int requestCode, int resultCode, Intent data )
	{
		if( requestCode != SHARE_RESULT_CODE )
			return;

		if( NativeShare.shareResultReceiver != null )
		{
			Log.d( "Unity", "Reported share result (may not be correct): " + ( resultCode == Activity.RESULT_OK ) );

			if( resultCode == Activity.RESULT_OK )
				NativeShare.shareResultReceiver.OnShareCompleted( 1, "" ); // 1: Shared
			else
			{
				if( Build.VERSION.SDK_INT < 22 )
				{
					// On older Android versions, unfortunately we can't determine whether or not user has picked an app
					// from the share sheet
					NativeShare.shareResultReceiver.OnShareCompleted( 0, "" ); // 0: Unknown
				}
				else
				{
					// On newer Android versions, it is safe to send NotShared result since for a successful share,
					// ShareResultBroadcastReceiver will override the result
					NativeShare.shareResultReceiver.OnShareCompleted( 2, "" ); // 2: NotShared
				}
			}
		}
		else
			Log.e( "Unity", "NativeShareResultReceiver was null!" );

		getFragmentManager().beginTransaction().remove( this ).commit();
	}
}