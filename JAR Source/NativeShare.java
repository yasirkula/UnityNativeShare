package com.yasirkula.unity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;

/**
 * Created by yasirkula on 22.06.2017.
 */

public class NativeShare
{
	private static String authority = null;

	public static void Share( Context context, String mediaPath, String subject, String message, String authority, boolean isMediaImage )
	{
		Intent intent = new Intent( Intent.ACTION_SEND );

		if( subject != null && subject.length() > 0 )
			intent.putExtra( Intent.EXTRA_SUBJECT, subject );

		if( message != null && message.length() > 0 )
			intent.putExtra( Intent.EXTRA_TEXT, message );

		String mimeType;
		if( mediaPath != null && mediaPath.length() > 0 )
		{
			Uri contentUri = UnitySSContentProvider.getUriForFile( context, authority, new File( mediaPath ) );
			intent.putExtra( Intent.EXTRA_STREAM, contentUri );

			if( isMediaImage )
				mimeType = "image/*";
			else
				mimeType = "video/mp4";
		}
		else
			mimeType = "text/plain";

		intent.setType( mimeType );
		intent.setFlags( Intent.FLAG_GRANT_READ_URI_PERMISSION );

		context.startActivity( Intent.createChooser( intent, "" ) );
	}
}
