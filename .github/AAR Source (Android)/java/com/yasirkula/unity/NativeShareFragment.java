package com.yasirkula.unity;

import android.app.Activity;
import android.app.Fragment;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

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
	public static final String EMAIL_RECIPIENTS_ID = "NS_EMAIL_RECIPIENTS";
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
			final ArrayList<Uri> fileUris = new ArrayList<Uri>();
			final Intent shareIntent = NativeShare.CreateIntentFromBundle( getActivity(), getArguments(), fileUris );
			final String title = getArguments().getString( NativeShareFragment.TITLE_ID );

			shareIntent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );

			try
			{
				Intent chooserIntent;
				if( Build.VERSION.SDK_INT < 22 )
					chooserIntent = Intent.createChooser( shareIntent, title );
				else
					chooserIntent = Intent.createChooser( shareIntent, title, NativeShareBroadcastListener.Initialize( getActivity() ) );

				if( fileUris.size() > 0 )
					NativeShare.GrantURIPermissionsToShareIntentTargets( getActivity(), getActivity().getPackageManager().queryIntentActivities( chooserIntent, PackageManager.MATCH_DEFAULT_ONLY ), fileUris );

				startActivityForResult( chooserIntent, SHARE_RESULT_CODE );
			}
			catch( ActivityNotFoundException e )
			{
				Toast.makeText( getActivity(), "No apps can perform this action.", Toast.LENGTH_LONG ).show();
				onActivityResult( SHARE_RESULT_CODE, Activity.RESULT_CANCELED, null );
			}
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