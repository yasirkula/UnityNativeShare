package com.yasirkula.unity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.util.Log;

// On Android 22 and newer devices, when standard ACTION_SEND share sheet is used, this BroadcastReceiver receives
// a message when an app from the share sheet is selected and then passes that information to Unity
@TargetApi( 22 )
public class NativeShareBroadcastListener extends BroadcastReceiver
{
	private static final String BROADCAST_RECEIVER_FILTER = "com.yasirkula.unity.NATIVESHARE_RESULTRECEIVER";

	private static NativeShareBroadcastListener broadcastReceiver;

	public static IntentSender Initialize( Context context )
	{
		if( broadcastReceiver == null )
		{
			broadcastReceiver = new NativeShareBroadcastListener();
			( (Activity) context ).getApplication().registerReceiver( broadcastReceiver, new IntentFilter( BROADCAST_RECEIVER_FILTER ) );
		}

		Intent receiverIntent = new Intent( BROADCAST_RECEIVER_FILTER );
		return PendingIntent.getBroadcast( context, 0, receiverIntent, PendingIntent.FLAG_UPDATE_CURRENT ).getIntentSender();
	}

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

		if( NativeShare.shareResultReceiver != null )
		{
			ComponentName selectedShareTarget = intent.getParcelableExtra( Intent.EXTRA_CHOSEN_COMPONENT );
			if( selectedShareTarget != null )
			{
				String selectedShareTargetStr = selectedShareTarget.flattenToString();
				Log.d( "Unity", "Shared on app: " + selectedShareTargetStr );

				NativeShare.shareResultReceiver.OnShareCompleted( 1, selectedShareTargetStr ); // 1: Shared
			}
			else
			{
				Log.d( "Unity", "Shared on app: Unknown" );
				NativeShare.shareResultReceiver.OnShareCompleted( 1, "" ); // 1: Shared
			}
		}
		else
			Log.e( "Unity", "NativeShareResultReceiver was null!" );
	}
}