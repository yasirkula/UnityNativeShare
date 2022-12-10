#import <UIKit/UIKit.h>
#import <Foundation/Foundation.h>
#ifdef UNITY_4_0 || UNITY_5_0
#import "iPhone_View.h"
#else
extern UIViewController* UnityGetGLViewController();
#endif

#define CHECK_IOS_VERSION( version )  ([[[UIDevice currentDevice] systemVersion] compare:version options:NSNumericSearch] != NSOrderedAscending)

// Credit: https://github.com/ChrisMaire/unity-native-sharing

// Credit: https://stackoverflow.com/a/29916845/2373034
@interface UNativeShareEmailItemProvider : NSObject <UIActivityItemSource>
@property (nonatomic, strong) NSString *subject;
@property (nonatomic, strong) NSString *body;
@end

// Credit: https://stackoverflow.com/a/29916845/2373034
@implementation UNativeShareEmailItemProvider
- (id)activityViewControllerPlaceholderItem:(UIActivityViewController *)activityViewController
{
	return [self body];
}

- (id)activityViewController:(UIActivityViewController *)activityViewController itemForActivityType:(NSString *)activityType
{
	return [self body];
}

- (NSString *)activityViewController:(UIActivityViewController *)activityViewController subjectForActivityType:(NSString *)activityType
{
	return [self subject];
}
@end

extern "C" void _NativeShare_Share( const char* files[], int filesCount, const char* subject, const char* text, const char* link ) 
{
	NSMutableArray *items = [NSMutableArray new];
	
	// When there is a subject on iOS 7 or later, text is provided together with subject via a UNativeShareEmailItemProvider
	// Credit: https://stackoverflow.com/a/29916845/2373034
	if( strlen( subject ) > 0 && CHECK_IOS_VERSION( @"7.0" ) )
	{
		UNativeShareEmailItemProvider *emailItem = [UNativeShareEmailItemProvider new];
		emailItem.subject = [NSString stringWithUTF8String:subject];
		emailItem.body = [NSString stringWithUTF8String:text];
		
		[items addObject:emailItem];
	}
	else if( strlen( text ) > 0 )
		[items addObject:[NSString stringWithUTF8String:text]];
	
	// Credit: https://forum.unity.com/threads/native-share-for-android-ios-open-source.519865/page-13#post-6942362
	if( strlen( link ) > 0 )
	{
		NSString *urlRaw = [NSString stringWithUTF8String:link];
		NSURL *url = [NSURL URLWithString:urlRaw];
		if( url == nil )
		{
			// Try escaping the URL
			if( CHECK_IOS_VERSION( @"9.0" ) )
			{
				url = [NSURL URLWithString:[urlRaw stringByAddingPercentEncodingWithAllowedCharacters:[NSCharacterSet URLHostAllowedCharacterSet]]];
				if( url == nil )
					url = [NSURL URLWithString:[urlRaw stringByAddingPercentEncodingWithAllowedCharacters:[NSCharacterSet URLFragmentAllowedCharacterSet]]];
			}
			else
				url = [NSURL URLWithString:[urlRaw stringByAddingPercentEscapesUsingEncoding:NSUTF8StringEncoding]];
		}
		
		if( url != nil )
			[items addObject:url];
		else
			NSLog( @"Couldn't create a URL from link: %@", urlRaw );
	}
	
	for( int i = 0; i < filesCount; i++ ) 
	{
		NSString *filePath = [NSString stringWithUTF8String:files[i]];
		UIImage *image = [UIImage imageWithContentsOfFile:filePath];
		if( image != nil )
			[items addObject:image];
		else
			[items addObject:[NSURL fileURLWithPath:filePath]];
	}
	
	if( strlen( subject ) == 0 && [items count] == 0 )
	{
		NSLog( @"Share canceled because there is nothing to share..." );
		UnitySendMessage( "NSShareResultCallbackiOS", "OnShareCompleted", "2" );
		
		return;
	}
	
	UIActivityViewController *activity = [[UIActivityViewController alloc] initWithActivityItems:items applicationActivities:nil];
	if( strlen( subject ) > 0 )
		[activity setValue:[NSString stringWithUTF8String:subject] forKey:@"subject"];
	
	void (^shareResultCallback)(UIActivityType activityType, BOOL completed, UIActivityViewController *activityReference) = ^void( UIActivityType activityType, BOOL completed, UIActivityViewController *activityReference )
	{
		NSLog( @"Shared to %@ with result: %d", activityType, completed );
		
		if( activityReference )
		{
			const char *resultMessage = [[NSString stringWithFormat:@"%d%@", completed ? 1 : 2, activityType] UTF8String];
			char *result = (char*) malloc( strlen( resultMessage ) + 1 );
			strcpy( result, resultMessage );
			
			UnitySendMessage( "NSShareResultCallbackiOS", "OnShareCompleted", result );
			
			// On iPhones, the share sheet isn't dismissed automatically when share operation is canceled, do that manually here
			if( !completed && UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPhone )
				[activityReference dismissViewControllerAnimated:NO completion:nil];
		}
		else
			NSLog( @"Share result callback is invoked multiple times!" );
	};
	
	if( CHECK_IOS_VERSION( @"8.0" ) )
	{
		__block UIActivityViewController *activityReference = activity; // About __block usage: https://gist.github.com/HawkingOuYang/b2c9783c75f929b5580c
		activity.completionWithItemsHandler = ^( UIActivityType activityType, BOOL completed, NSArray *returnedItems, NSError *activityError )
		{
			if( activityError != nil )
				NSLog( @"Share error: %@", activityError );
			
			shareResultCallback( activityType, completed, activityReference );
			activityReference = nil;
		};
	}
	else if( CHECK_IOS_VERSION( @"6.0" ) )
	{
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wdeprecated-declarations"
		__block UIActivityViewController *activityReference = activity;
		activity.completionHandler = ^( UIActivityType activityType, BOOL completed )
		{
			shareResultCallback( activityType, completed, activityReference );
			activityReference = nil;
		};
#pragma clang diagnostic pop
	}
	else
		UnitySendMessage( "NSShareResultCallbackiOS", "OnShareCompleted", "" );
	
	UIViewController *rootViewController = UnityGetGLViewController();
	if( UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPhone ) // iPhone
	{
		[rootViewController presentViewController:activity animated:YES completion:nil];
	}
	else // iPad
	{
		UIPopoverController *popup = [[UIPopoverController alloc] initWithContentViewController:activity];
		[popup presentPopoverFromRect:CGRectMake( rootViewController.view.frame.size.width / 2, rootViewController.view.frame.size.height / 2, 1, 1 ) inView:rootViewController.view permittedArrowDirections:0 animated:YES];
	}
}