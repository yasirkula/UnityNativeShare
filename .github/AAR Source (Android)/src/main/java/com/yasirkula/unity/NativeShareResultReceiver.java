package com.yasirkula.unity;

/**
 * Created by yasirkula on 11.07.2020.
 */

public interface NativeShareResultReceiver
{
	void OnShareCompleted( int result, String shareTarget );
	boolean HasManagedCallback();
}