package com.yasirkula.unity;

import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.os.PersistableBundle;

// Sole purpose of this activity is to show a NativeShareCustomShareDialog inside
// We are not displaying this dialog inside Unity's own Activity for 2 reasons:
// 1: When shown inside Unity'a activity, the dialog doesn't block Unity's main thread (game keeps playing in the background)
// (although this also happens on iOS, this behaviour is inconsistent with the standard ACTION_SEND intent)
// 2: More importantly, it takes a couple of seconds before the dialog is shown. During that time, user may think that they
// didn't click the Share button and click it again. This will result in 2 dialogs being displayed one after another and the
// second dialog's result can't be fetched by Unity
// Using a separate Activity (this one) helps eliminate both of these issues
public class NativeShareCustomShareDialogActivity extends Activity
{
	private boolean dialogDisplayed;

	@Override
	public void onCreate( Bundle savedInstanceState, PersistableBundle persistentState )
	{
		super.onCreate( savedInstanceState, persistentState );
		setContentView( R.layout.native_share_custom_dialog_holder );
	}

	@Override
	protected void onStart()
	{
		super.onStart();

		if( !dialogDisplayed )
		{
			dialogDisplayed = true;

			final DialogFragment request = new NativeShareCustomShareDialog();
			request.setArguments( getIntent().getExtras() );
			getFragmentManager().beginTransaction().add( 0, request ).commit();
			//request.show( getFragmentManager(), "customsharedialog" );
		}
		else
			finish();
	}
}