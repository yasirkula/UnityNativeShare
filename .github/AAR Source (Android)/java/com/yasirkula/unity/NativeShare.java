package com.yasirkula.unity;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Created by yasirkula on 22.06.2017.
 */

public class NativeShare
{
	public static String authority = null;
	public static NativeShareResultReceiver shareResultReceiver;

	public static boolean alwaysUseCustomShareDialog = false;

	private static int isXiaomiOrMIUI = 0; // 1: true, -1: false

	public static void Share( final Context context, final NativeShareResultReceiver shareResultReceiver, final String[] targetPackages, final String[] targetClasses, final String[] files, final String[] mimes, final String[] emailRecipients, final String subject, final String text, final String title )
	{
		if( files.length > 0 && GetAuthority( context ) == null )
		{
			Log.e( "Unity", "Can't find ContentProvider, share not possible!" );
			shareResultReceiver.OnShareCompleted( 2, "" ); // 2: NotShared

			return;
		}

		NativeShare.shareResultReceiver = shareResultReceiver;

		Bundle bundle = new Bundle();
		bundle.putString( NativeShareFragment.SUBJECT_ID, subject );
		bundle.putString( NativeShareFragment.TEXT_ID, text );
		bundle.putString( NativeShareFragment.TITLE_ID, title );
		bundle.putStringArrayList( NativeShareFragment.FILES_ID, ConvertArrayToArrayList( files ) );
		bundle.putStringArrayList( NativeShareFragment.MIMES_ID, ConvertArrayToArrayList( mimes ) );
		bundle.putStringArrayList( NativeShareFragment.EMAIL_RECIPIENTS_ID, ConvertArrayToArrayList( emailRecipients ) );
		bundle.putStringArrayList( NativeShareFragment.TARGET_PACKAGE_ID, ConvertArrayToArrayList( targetPackages ) );
		bundle.putStringArrayList( NativeShareFragment.TARGET_CLASS_ID, ConvertArrayToArrayList( targetClasses ) );

		boolean shouldUseCustomShareDialog = alwaysUseCustomShareDialog || targetPackages.length > 1;
		if( !shouldUseCustomShareDialog && shareResultReceiver.HasManagedCallback() )
		{
			if( Build.VERSION.SDK_INT < 22 )
				shouldUseCustomShareDialog = true; // Old devices don't support callback unless custom dialog is used
			else if( "huawei".equalsIgnoreCase( android.os.Build.MANUFACTURER ) )
				shouldUseCustomShareDialog = true; // At least some Huawei devices don't support callback for unknown reasons
		}

		// 1) MIUI devices have issues with Intent.createChooser on at least Android 11 (https://stackoverflow.com/questions/67785661/taking-and-picking-photos-on-poco-x3-with-android-11-does-not-work)
		// 2) At least some Xiaomi devices can't display share dialog properly when in landscape mode (Issue #56)
		if( !shouldUseCustomShareDialog && IsXiaomiOrMIUI() && ( Build.VERSION.SDK_INT == 30 || IsUnityInLandscapeMode( (Activity) context ) ) )
			shouldUseCustomShareDialog = true;

		if( shouldUseCustomShareDialog )
		{
			Log.d( "Unity", "Creating custom share dialog" );

			Intent request = new Intent( context, NativeShareCustomShareDialogActivity.class );
			request.putExtras( bundle );
			request.setFlags( Intent.FLAG_ACTIVITY_NO_HISTORY );

			context.startActivity( request );
		}
		else
		{
			Log.d( "Unity", "Creating standard share dialog" );

			final Fragment request = new NativeShareFragment();
			request.setArguments( bundle );

			( (Activity) context ).getFragmentManager().beginTransaction().add( 0, request ).commit();
		}
	}

	public static Intent CreateIntentFromBundle( Context context, Bundle bundle, ArrayList<Uri> fileUris )
	{
		final String subject = bundle.getString( NativeShareFragment.SUBJECT_ID );
		final String text = bundle.getString( NativeShareFragment.TEXT_ID );
		final String title = bundle.getString( NativeShareFragment.TITLE_ID );
		final ArrayList<String> files = bundle.getStringArrayList( NativeShareFragment.FILES_ID );
		final ArrayList<String> mimes = bundle.getStringArrayList( NativeShareFragment.MIMES_ID );
		final ArrayList<String> emailRecipients = bundle.getStringArrayList( NativeShareFragment.EMAIL_RECIPIENTS_ID );
		final ArrayList<String> targetPackages = bundle.getStringArrayList( NativeShareFragment.TARGET_PACKAGE_ID );
		final ArrayList<String> targetClasses = bundle.getStringArrayList( NativeShareFragment.TARGET_CLASS_ID );

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
					if( extensionStart < 0 || extensionStart == files.get( i ).length() - 1 )
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

				Uri contentUri = NativeShareContentProvider.getUriForFile( context, NativeShare.authority, new File( files.get( 0 ) ) );
				fileUris.add( contentUri );

				intent.putExtra( Intent.EXTRA_STREAM, contentUri );
			}
			else
			{
				// Credit: https://stackoverflow.com/a/27514002/2373034
				intent.setAction( Intent.ACTION_SEND_MULTIPLE );
				for( int i = 0; i < files.size(); i++ )
				{
					Uri contentUri = NativeShareContentProvider.getUriForFile( context, NativeShare.authority, new File( files.get( i ) ) );
					fileUris.add( contentUri );
				}

				intent.putParcelableArrayListExtra( Intent.EXTRA_STREAM, fileUris );
			}
		}
		else
		{
			mime = "text/plain";
			intent.setAction( Intent.ACTION_SEND );
		}

		if( emailRecipients.size() > 0 )
		{
			String[] emailRecipientsArray = new String[emailRecipients.size()];
			emailRecipients.toArray( emailRecipientsArray );
			intent.putExtra( Intent.EXTRA_EMAIL, emailRecipientsArray );
		}

		if( title.length() > 0 )
			intent.putExtra( Intent.EXTRA_TITLE, title );

		intent.setType( mime );
		intent.setFlags( Intent.FLAG_GRANT_READ_URI_PERMISSION );

		if( targetPackages.size() == 1 )
		{
			intent.setPackage( targetPackages.get( 0 ) );

			if( targetClasses.get( 0 ).length() > 0 )
				intent.setClassName( targetPackages.get( 0 ), targetClasses.get( 0 ) );
		}

		return intent;
	}

	public static void GrantURIPermissionsToShareIntentTargets( Context context, List<ResolveInfo> shareTargets, ArrayList<Uri> fileUris )
	{
		// Avoid "java.lang.SecurityException: Permission Denial: reading com.yasirkula.unity.NativeShareContentProvider uri ... requires the provider be exported, or grantUriPermission()"
		// Credit: https://stackoverflow.com/a/59439316/2373034
		try
		{
			for( int i = shareTargets.size() - 1; i >= 0; i-- )
			{
				for( int j = fileUris.size() - 1; j >= 0; j-- )
					context.grantUriPermission( shareTargets.get( i ).activityInfo.packageName, fileUris.get( j ), Intent.FLAG_GRANT_READ_URI_PERMISSION );
			}
		}
		catch( Exception e )
		{
			Log.e( "Unity", "NativeShare couldn't call grantUriPermission:", e );
		}
	}

	private static String GetAuthority( Context context )
	{
		if( authority == null )
		{
			// Find the authority of ContentProvider first
			// Credit: https://stackoverflow.com/a/2001769/2373034
			try
			{
				PackageInfo packageInfo = context.getPackageManager().getPackageInfo( context.getPackageName(), PackageManager.GET_PROVIDERS );
				ProviderInfo[] providers = packageInfo.providers;
				if( providers != null )
				{
					for( ProviderInfo provider : providers )
					{
						if( provider.name != null && provider.packageName != null && provider.authority != null &&
								provider.name.equals( NativeShareContentProvider.class.getName() ) && provider.packageName.equals( context.getPackageName() )
								&& provider.authority.length() > 0 )
						{
							authority = provider.authority;
							break;
						}
					}
				}
			}
			catch( Exception e )
			{
				Log.e( "Unity", "Exception:", e );
			}
		}

		return authority;
	}

	private static ArrayList<String> ConvertArrayToArrayList( String[] arr )
	{
		ArrayList<String> result = new ArrayList<String>( arr.length );
		for( int i = 0; i < arr.length; i++ )
			result.add( arr[i] );

		return result;
	}

	private static boolean IsUnityInLandscapeMode( Activity unityActivity )
	{
		return unityActivity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
	}

	private static boolean IsXiaomiOrMIUI()
	{
		if( isXiaomiOrMIUI > 0 )
			return true;
		else if( isXiaomiOrMIUI < 0 )
			return false;

		if( "xiaomi".equalsIgnoreCase( android.os.Build.MANUFACTURER ) )
		{
			isXiaomiOrMIUI = 1;
			return true;
		}

		// Check if device is using MIUI
		// Credit: https://gist.github.com/Muyangmin/e8ec1002c930d8df3df46b306d03315d
		String line;
		BufferedReader inputStream = null;
		try
		{
			java.lang.Process process = Runtime.getRuntime().exec( "getprop ro.miui.ui.version.name" );
			inputStream = new BufferedReader( new InputStreamReader( process.getInputStream() ), 1024 );
			line = inputStream.readLine();

			if( line != null && line.length() > 0 )
			{
				isXiaomiOrMIUI = 1;
				return true;
			}
			else
			{
				isXiaomiOrMIUI = -1;
				return false;
			}
		}
		catch( Exception e )
		{
			isXiaomiOrMIUI = -1;
			return false;
		}
		finally
		{
			if( inputStream != null )
			{
				try
				{
					inputStream.close();
				}
				catch( Exception e )
				{
				}
			}
		}
	}

	public static boolean TargetExists( Context context, String packageName, String className )
	{
		try
		{
			if( className.length() == 0 )
			{
				context.getPackageManager().getPackageInfo( packageName, 0 );
				return true;
			}

			PackageInfo packageInfo = context.getPackageManager().getPackageInfo( packageName, PackageManager.GET_ACTIVITIES );
			ActivityInfo[] activities = packageInfo.activities;
			if( activities != null )
			{
				for( ActivityInfo activityInfo : activities )
				{
					if( activityInfo.name.equals( className ) )
						return true;
				}
			}

			return false;
		}
		catch( PackageManager.NameNotFoundException e )
		{
			return false;
		}
	}

	public static String FindMatchingTarget( Context context, String packageNameRegex, String classNameRegex )
	{
		List<PackageInfo> packages = context.getPackageManager().getInstalledPackages( PackageManager.GET_ACTIVITIES );
		if( packages != null )
		{
			Pattern packagePattern = Pattern.compile( packageNameRegex );
			Pattern classPattern = classNameRegex.length() > 0 ? Pattern.compile( classNameRegex ) : null;

			for( PackageInfo packageInfo : packages )
			{
				if( packagePattern.matcher( packageInfo.packageName ).find() )
				{
					ActivityInfo[] activities = packageInfo.activities;
					if( activities != null )
					{
						for( ActivityInfo activityInfo : activities )
						{
							if( classPattern == null || classPattern.matcher( activityInfo.name ).find() )
								return packageInfo.packageName + ">" + activityInfo.name;
						}
					}
				}
			}
		}

		return "";
	}
}