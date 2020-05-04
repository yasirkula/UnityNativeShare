#import <UIKit/UIKit.h>
#import <Foundation/Foundation.h>
#ifdef UNITY_4_0 || UNITY_5_0
#import "iPhone_View.h"
#else
extern UIViewController* UnityGetGLViewController();
#endif

@interface PlatformDependentMediaProvider:UIActivityItemProvider <UIActivityItemSource>
@property (nonatomic, copy) NSString *filePath;
- (PlatformDependentMediaProvider *)initWithFilePath:(NSString *)filePath;
@end
@implementation PlatformDependentMediaProvider
@synthesize filePath;

- (PlatformDependentMediaProvider *)initWithFilePath:(NSString *)filePath {
    if (self = [super initWithPlaceholderItem:filePath]) {
        self.filePath = filePath;
    }
    return self;
}

- (id)activityViewControllerPlaceholderItem:(UIActivityViewController *)activityViewController {
    return [[UIImage alloc] init];
}

- (id)activityViewController:(UIActivityViewController *)activityViewController itemForActivityType:(UIActivityType)activityType {
    if ([activityType isEqualToString:@"com.facebook.Messenger.ShareExtension"]) {
        return filePath.length != 0 ? [[UIImage alloc] initWithContentsOfFile:filePath] : nil;
    }
    
    return filePath.length != 0 ? [NSURL fileURLWithPath:filePath] : nil;
}
@end

@interface OptionalPlatformDependentMediaProvider:UIActivityItemProvider <UIActivityItemSource>
@property (nonatomic, copy) NSString *filePath;
- (OptionalPlatformDependentMediaProvider *)initWithFilePath:(NSString *)filePath;
@end
@implementation OptionalPlatformDependentMediaProvider
@synthesize filePath;

- (OptionalPlatformDependentMediaProvider *)initWithFilePath:(NSString *)filePath {
    if (self = [super initWithPlaceholderItem:filePath]) {
        self.filePath = filePath;
    }
    return self;
}

- (id)activityViewControllerPlaceholderItem:(UIActivityViewController *)activityViewController {
    return [[UIImage alloc] init];
}

- (id)activityViewController:(UIActivityViewController *)activityViewController itemForActivityType:(UIActivityType)activityType {
    if ([activityType isEqualToString:UIActivityTypePostToFacebook]) {
        return nil;
    } else if ([activityType isEqualToString:UIActivityTypePostToTwitter]) {
        return nil;
    } else if ([activityType isEqualToString:@"com.facebook.Messenger.ShareExtension"]) {
        return filePath.length != 0 ? [[UIImage alloc] initWithContentsOfFile:filePath] : nil;
    }
    
    return filePath.length != 0 ? [NSURL fileURLWithPath:filePath] : nil;
}
@end

@interface OptionalPlatformDependentImageProvider:UIActivityItemProvider <UIActivityItemSource>
@property (nonatomic, copy) UIImage *image;
- (OptionalPlatformDependentImageProvider *)initWithImage:(UIImage *)image;
@end
@implementation OptionalPlatformDependentImageProvider
@synthesize image;

- (OptionalPlatformDependentImageProvider *)initWithImage:(UIImage *)image {
    if (self = [super initWithPlaceholderItem:image]) {
        self.image = image;
    }
    return self;
}

- (id)activityViewControllerPlaceholderItem:(UIActivityViewController *)activityViewController {
    return image;
}

- (id)activityViewController:(UIActivityViewController *)activityViewController itemForActivityType:(UIActivityType)activityType {
    if ([activityType isEqualToString:UIActivityTypePostToFacebook]) {
        return nil;
    } else if ([activityType isEqualToString:UIActivityTypePostToTwitter]) {
        return nil;
    } else if ([activityType isEqualToString:@"com.facebook.Messenger.ShareExtension"]) {
        return image;
    }
    
    return image;
}
@end

@interface OptionalPlatformDependentUrlProvider:UIActivityItemProvider <UIActivityItemSource>
@property (nonatomic, copy) NSString *urlPath;
- (OptionalPlatformDependentUrlProvider *)initWithUrlPath:(NSString *)urlPath;
@end
@implementation OptionalPlatformDependentUrlProvider
@synthesize urlPath;

- (OptionalPlatformDependentUrlProvider *)initWithUrlPath:(NSString *)urlPath {
    if (self = [super initWithPlaceholderItem:urlPath]) {
        self.urlPath = urlPath;
    }
    return self;
}

- (id)activityViewControllerPlaceholderItem:(UIActivityViewController *)activityViewController {
    return urlPath.length != 0 ? [[NSURL alloc] initWithString:urlPath] : [[NSURL alloc] initWithString:@""];
}

- (id)activityViewController:(UIActivityViewController *)activityViewController itemForActivityType:(UIActivityType)activityType {
    if ([activityType isEqualToString:UIActivityTypePostToFacebook]) {
        return nil;
    } else if ([activityType isEqualToString:UIActivityTypePostToTwitter]) {
        return nil;
    } else if ([activityType isEqualToString:@"com.facebook.Messenger.ShareExtension"]) {
        return nil;
    }
    
    return urlPath.length != 0 ? [[NSURL alloc] initWithString:urlPath] : nil;
}
@end

// Credit: https://github.com/ChrisMaire/unity-native-sharing

extern "C" void _NativeShare_Share( const char* files[], int filesCount, char* subject, const char* text, const char* url, bool prioritizeFile ) 
{
	NSMutableArray *items = [NSMutableArray new];
	if(filesCount == 0 || !prioritizeFile)
		[items addObject:[[NSURL alloc] initWithString: [NSString stringWithUTF8String:url]]];
	else if( url && strlen( url ) > 0 )
		[items addObject:[[OptionalPlatformDependentUrlProvider alloc] initWithUrlPath: [NSString stringWithUTF8String:url]]];

	if( text && strlen( text ) > 0 )
		[items addObject:[NSString stringWithUTF8String:text]];

	// Credit: https://answers.unity.com/answers/862224/view.html
	for( int i = 0; i < filesCount; i++ ) 
	{
		NSString *filePath = [NSString stringWithUTF8String:files[i]];
		UIImage *image = [UIImage imageWithContentsOfFile:filePath];
		if(prioritizeFile) {
			if( image )
				[items addObject:image];
			else
				[items addObject:[[PlatformDependentMediaProvider alloc] initWithFilePath:filePath]];
		} else {
			if( image )
				[items addObject:[[OptionalPlatformDependentImageProvider alloc] initWithImage:image]];
			else
				[items addObject:[[OptionalPlatformDependentMediaProvider alloc] initWithFilePath:filePath]];
		}
	}

	UIActivityViewController *activity = [[UIActivityViewController alloc] initWithActivityItems:items applicationActivities:nil];
	if( subject && strlen( subject ) > 0 )
		[activity setValue:[NSString stringWithUTF8String:subject] forKey:@"subject"];

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
