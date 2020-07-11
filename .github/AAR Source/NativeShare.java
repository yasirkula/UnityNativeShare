package com.yasirkula.unity;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.os.Bundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by yasirkula on 22.06.2017.
 */

public class NativeShare
{
	public static String authority = null;

	public static void Share( Context context, final NativeShareResultReceiver shareResultReceiver, final String targetPackage, final String targetClass, final String[] files, final String[] mimes, final String subject, final String text, final String title )
	{
		if( files.length > 0 && GetAuthority( context ) == null )
		{
			Log.e( "Unity", "Can't find ContentProvider, share not possible!" );
			shareResultReceiver.OnShareCompleted( 2, "" ); // 2: NotShared

			return;
		}

		ArrayList<String> filesList = new ArrayList<String>( files.length );
		for( int i = 0; i < files.length; i++ )
			filesList.add( files[i] );

		ArrayList<String> mimesList = new ArrayList<String>( mimes.length );
		for( int i = 0; i < mimes.length; i++ )
			mimesList.add( mimes[i] );

		Bundle bundle = new Bundle();
		bundle.putString( NativeShareFragment.TARGET_PACKAGE_ID, targetPackage );
		bundle.putString( NativeShareFragment.TARGET_CLASS_ID, targetClass );
		bundle.putString( NativeShareFragment.SUBJECT_ID, subject );
		bundle.putString( NativeShareFragment.TEXT_ID, text );
		bundle.putString( NativeShareFragment.TITLE_ID, title );
		bundle.putStringArrayList( NativeShareFragment.FILES_ID, filesList );
		bundle.putStringArrayList( NativeShareFragment.MIMES_ID, mimesList );

		final Fragment request = new NativeShareFragment( shareResultReceiver );
		request.setArguments( bundle );

		( (Activity) context ).getFragmentManager().beginTransaction().add( 0, request ).commit();
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