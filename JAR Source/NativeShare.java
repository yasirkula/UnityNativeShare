package com.yasirkula.unity;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by yasirkula on 22.06.2017.
 */

public class NativeShare
{
	public static void Share( Context context, String[] files, String[] mimes, String subject, String text, String title, String authority )
	{
		Intent intent = new Intent();

		if( subject.length() > 0 )
			intent.putExtra( Intent.EXTRA_SUBJECT, subject );

		if( text.length() > 0 )
			intent.putExtra( Intent.EXTRA_TEXT, text );

		String mime;
		if( files.length > 0 )
		{
			String mimeType = null;
			String mimeSubtype = null;
			for( int i = 0; i < files.length; i++ )
			{
				String thisMime;
				if( mimes[i].length() > 0 )
					thisMime = mimes[i];
				else
				{
					int extensionStart = files[i].lastIndexOf( '.' );
					if( extensionStart < 0 || extensionStart == files.length - 1 )
					{
						mimeType = mimeSubtype = "*";
						break;
					}

					// Credit: https://stackoverflow.com/a/31691791/2373034
					thisMime = MimeTypeMap.getSingleton().getMimeTypeFromExtension( files[i].substring( extensionStart + 1 ).toLowerCase( Locale.ENGLISH ) );
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

			if( files.length == 1 )
			{
				intent.setAction( Intent.ACTION_SEND );

				Uri contentUri = UnitySSContentProvider.getUriForFile( context, authority, new File( files[0] ) );
				intent.putExtra( Intent.EXTRA_STREAM, contentUri );
			}
			else
			{
				// Credit: https://stackoverflow.com/a/27514002/2373034
				intent.setAction( Intent.ACTION_SEND_MULTIPLE );
				ArrayList<Uri> uris = new ArrayList<Uri>( files.length );
				for( int i = 0; i < files.length; i++ )
				{
					Uri contentUri = UnitySSContentProvider.getUriForFile( context, authority, new File( files[i] ) );
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

		context.startActivity( Intent.createChooser( intent, title ) );
	}
}