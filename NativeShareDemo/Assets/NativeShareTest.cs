using System.Collections;
using System.IO;
using UnityEngine;

public class NativeShareTest : MonoBehaviour
{
	void Update()
	{
		transform.Rotate( 0, 90 * Time.deltaTime, 0 );
		if( Input.GetMouseButtonDown( 0 ) )
			StartCoroutine( TakeSSAndShare() );
	}
	
	private IEnumerator TakeSSAndShare()
	{
		yield return new WaitForEndOfFrame();

		Texture2D ss = new Texture2D( Screen.width, Screen.height, TextureFormat.RGB24, false );
		ss.ReadPixels( new Rect( 0, 0, Screen.width, Screen.height ), 0, 0 );
		ss.Apply();

		string filePath = Path.Combine( Application.persistentDataPath, "shared img.png" );
        File.WriteAllBytes( filePath, ss.EncodeToPNG() );

		NativeShare.Share( filePath, true, "Mi.ShareIt" );
    }
}
