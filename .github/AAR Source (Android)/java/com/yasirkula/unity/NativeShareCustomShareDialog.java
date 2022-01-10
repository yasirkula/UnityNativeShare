package com.yasirkula.unity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Displays custom share dialog that supports callbacks on all Android versions, supports multiple share targets and
// have consistent behaviour across all Android devices and screen orientations
public class NativeShareCustomShareDialog extends DialogFragment
{
	private static class CustomShareDialogAdapter extends ArrayAdapter<ResolveInfo>
	{
		private static class ViewHolder
		{
			private final TextView label;
			private final ImageView icon;

			public ViewHolder( TextView label, ImageView icon )
			{
				this.label = label;
				this.icon = icon;
			}
		}

		private Activity unityActivity;
		private PackageManager packageManager;

		private CustomShareDialogAdapter( Activity unityActivity, List<ResolveInfo> apps )
		{
			super( unityActivity, R.layout.native_share_grid_element, apps );

			this.unityActivity = unityActivity;
			packageManager = unityActivity.getPackageManager();
		}

		@Override
		public View getView( int position, View convertView, ViewGroup parent )
		{
			if( convertView == null )
			{
				convertView = unityActivity.getLayoutInflater().inflate( R.layout.native_share_grid_element, parent, false );
				convertView.setTag( new ViewHolder( (TextView) convertView.findViewById( R.id.native_share_app_name ), (ImageView) convertView.findViewById( R.id.native_share_app_icon ) ) );
			}

			ResolveInfo item = getItem( position );

			final ViewHolder viewHolder = (ViewHolder) convertView.getTag();
			viewHolder.label.setText( item.loadLabel( packageManager ) );
			viewHolder.icon.setImageDrawable( item.loadIcon( packageManager ) );

			return convertView;
		}
	}

	private boolean sentShareResult;

	@Override
	public Dialog onCreateDialog( Bundle savedInstanceState )
	{
		final ArrayList<Uri> fileUris = new ArrayList<Uri>();
		final Intent shareIntent = NativeShare.CreateIntentFromBundle( getActivity(), getArguments(), fileUris );
		final String title = getArguments().getString( NativeShareFragment.TITLE_ID );
		final ArrayList<String> targetPackages = getArguments().getStringArrayList( NativeShareFragment.TARGET_PACKAGE_ID );
		final ArrayList<String> targetClasses = getArguments().getStringArrayList( NativeShareFragment.TARGET_CLASS_ID );

		PackageManager packageManager = getActivity().getPackageManager();
		List<ResolveInfo> shareTargets = packageManager.queryIntentActivities( shareIntent, PackageManager.MATCH_DEFAULT_ONLY );

		if( fileUris.size() > 0 )
			NativeShare.GrantURIPermissionsToShareIntentTargets( getActivity(), shareTargets, fileUris );

		if( targetPackages.size() > 1 )
		{
			for( int i = shareTargets.size() - 1; i >= 0; i-- )
			{
				ActivityInfo targetActivity = shareTargets.get( i ).activityInfo;
				String packageName = targetActivity.applicationInfo.packageName;
				String className = targetActivity.name;

				boolean keepActivity = false;
				for( int j = targetPackages.size() - 1; j >= 0; j-- )
				{
					if( packageName.equals( targetPackages.get( j ) ) && ( targetClasses.get( j ).length() == 0 || className.equals( targetClasses.get( j ) ) ) )
					{
						keepActivity = true;
						break;
					}
				}

				if( !keepActivity )
					shareTargets.remove( i );
			}
		}

		if( shareTargets.size() == 0 )
		{
			return new AlertDialog.Builder( getActivity() )
					.setTitle( title )
					.setPositiveButton( android.R.string.ok, new DialogInterface.OnClickListener()
					{
						@Override
						public void onClick( DialogInterface dialog, int which )
						{
							dismiss();
						}
					} )
					.setMessage( "No apps can perform this action." ).create();
		}

		if( shareTargets.size() == 1 )
		{
			StartTargetActivity( shareIntent, shareTargets.get( 0 ).activityInfo );
			dismiss();
		}
		else
			Collections.sort( shareTargets, new ResolveInfo.DisplayNameComparator( packageManager ) );

		final CustomShareDialogAdapter shareDialogAdapter = new CustomShareDialogAdapter( getActivity(), shareTargets );
		final View gridViewHolder = getActivity().getLayoutInflater().inflate( R.layout.native_share_grid_view, null );
		final GridView gridView = (GridView) gridViewHolder.findViewById( R.id.native_share_grid_view_holder );
		gridView.setAdapter( shareDialogAdapter );
		gridView.setOnItemClickListener( new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick( AdapterView<?> parent, View view, int position, long id )
			{
				StartTargetActivity( shareIntent, shareDialogAdapter.getItem( position ).activityInfo );
				dismiss();
			}
		} );

		return new AlertDialog.Builder( getActivity() )
				.setTitle( title )
				.setNegativeButton( android.R.string.cancel, new DialogInterface.OnClickListener()
				{
					@Override
					public void onClick( DialogInterface dialog, int which )
					{
						dismiss();
					}
				} )
				.setView( gridViewHolder ).create();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		dismiss();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		if( !sentShareResult )
		{
			sentShareResult = true;

			// It is safe to send NotShared result here since for a successful share, NotShared won't override Shared
			NativeShare.shareResultReceiver.OnShareCompleted( 2, "" ); // 2: NotShared
		}
	}

	@Override
	public void onDismiss( DialogInterface dialog )
	{
		super.onDismiss( dialog );
		Log.d( "Unity", "Dismissed custom share dialog" );

		if( !sentShareResult )
		{
			sentShareResult = true;

			// It is safe to send NotShared result here since for a successful share, NotShared won't override Shared
			NativeShare.shareResultReceiver.OnShareCompleted( 2, "" ); // 2: NotShared
		}

		Activity ownerActivity = getActivity();
		if( ownerActivity != null && ownerActivity instanceof NativeShareCustomShareDialogActivity )
			ownerActivity.finish();
	}

	private void StartTargetActivity( final Intent shareIntent, final ActivityInfo targetActivity )
	{
		ComponentName shareTarget = new ComponentName( targetActivity.applicationInfo.packageName, targetActivity.name );
		String selectedShareTargetStr = shareTarget.flattenToString();
		Log.d( "Unity", "Shared on app: " + selectedShareTargetStr );

		NativeShare.shareResultReceiver.OnShareCompleted( 1, selectedShareTargetStr ); // 1: Shared
		sentShareResult = true;

		shareIntent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK );
		shareIntent.setComponent( shareTarget );

		startActivity( shareIntent );
	}
}