package com.yasirkula.unity;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Build;
import android.util.Log;

// On Android 22 and newer devices, when standard ACTION_SEND share sheet is used, this BroadcastReceiver receives
// a message when an app from the share sheet is selected and then passes that information to Unity
@TargetApi( 22 )
public class NativeShareBroadcastListener extends BroadcastReceiver
{
	public static IntentSender Initialize( Context context )
	{
		Intent receiverIntent = new Intent( context, NativeShareBroadcastListener.class );

		int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
		if( Build.VERSION.SDK_INT >= 31 )
		{
			// We must mark PendingIntent as either mutable or immutable on Android 12+
			// Maybe FLAG_IMMUTABLE is sufficient but the pre-31 default value was implicitly mutable and I don't trust
			// all social apps to work correctly on Android 12+ (API 31+) if I set it to FLAG_IMMUTABLE
			//pendingIntentFlags |= PendingIntent.FLAG_MUTABLE;

			// Only temporarily set the IMMUTABLE flag to avoid crashes until Android 12 SDK is officially released
			// https://github.com/yasirkula/UnityNativeShare/issues/130
			pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
		}

		return PendingIntent.getBroadcast( context, 0, receiverIntent, pendingIntentFlags ).getIntentSender();
	}

	@Override
	public void onReceive( Context context, Intent intent )
	{
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