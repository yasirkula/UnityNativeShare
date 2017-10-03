#ifdef UNITY_4_0 || UNITY_5_0

#import "iPhone_View.h"

#else

extern UIViewController* UnityGetGLViewController();

#endif

extern "C" void _ShareText(const char* subject, const char* message) 
{
	NSArray *items = @[[NSString stringWithUTF8String:message]];
	
	UIActivityViewController *activity = [[UIActivityViewController alloc] initWithActivityItems:items applicationActivities:Nil];
	if (strlen(subject) > 0)
		[activity setValue:[NSString stringWithUTF8String:subject] forKey:@"subject"];
	
	UIViewController *rootViewController = UnityGetGLViewController();
    //if iPhone
    if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPhone) {
          [rootViewController presentViewController:activity animated:YES completion:Nil];
    }
    //if iPad
    else {
        // Change Rect to position Popover
        UIPopoverController *popup = [[UIPopoverController alloc] initWithContentViewController:activity];
        [popup presentPopoverFromRect:CGRectMake(rootViewController.view.frame.size.width/2, rootViewController.view.frame.size.height/4, 0, 0)inView:rootViewController.view permittedArrowDirections:UIPopoverArrowDirectionAny animated:YES];
    }
}

extern "C" void _ShareImage(const char* path, const char* subject, const char* message) 
{
	UIImage *image = [UIImage imageWithContentsOfFile:[NSString stringWithUTF8String:path]];
	NSArray *items;
	if (strlen(message) > 0)
		items = @[image];
	else
		items = @[[NSString stringWithUTF8String:message], image];
	
	UIActivityViewController *activity = [[UIActivityViewController alloc] initWithActivityItems:items applicationActivities:Nil];
	if (strlen(subject) > 0)
		[activity setValue:[NSString stringWithUTF8String:subject] forKey:@"subject"];
	
	UIViewController *rootViewController = UnityGetGLViewController();
    //if iPhone
    if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPhone) {
          [rootViewController presentViewController:activity animated:YES completion:Nil];
    }
    //if iPad
    else {
        // Change Rect to position Popover
        UIPopoverController *popup = [[UIPopoverController alloc] initWithContentViewController:activity];
        [popup presentPopoverFromRect:CGRectMake(rootViewController.view.frame.size.width/2, rootViewController.view.frame.size.height/4, 0, 0)inView:rootViewController.view permittedArrowDirections:UIPopoverArrowDirectionAny animated:YES];
    }
}

extern "C" void _ShareVideo(const char* path, const char* subject, const char* message) 
{
	NSString *videoPath = [NSString stringWithUTF8String:path];
	NSURL *videoURL = [NSURL fileURLWithPath:videoPath];
	NSArray *items;
	if (strlen(message) > 0)
		items = @[videoURL];
	else
		items = @[[NSString stringWithUTF8String:message], videoURL];
	
	UIActivityViewController *activity = [[UIActivityViewController alloc] initWithActivityItems:items applicationActivities:Nil];
	if (strlen(subject) > 0)
		[activity setValue:[NSString stringWithUTF8String:subject] forKey:@"subject"];
	
	UIViewController *rootViewController = UnityGetGLViewController();
    //if iPhone
    if (UI_USER_INTERFACE_IDIOM() == UIUserInterfaceIdiomPhone) {
          [rootViewController presentViewController:activity animated:YES completion:Nil];
    }
    //if iPad
    else {
        // Change Rect to position Popover
        UIPopoverController *popup = [[UIPopoverController alloc] initWithContentViewController:activity];
        [popup presentPopoverFromRect:CGRectMake(rootViewController.view.frame.size.width/2, rootViewController.view.frame.size.height/4, 0, 0)inView:rootViewController.view permittedArrowDirections:UIPopoverArrowDirectionAny animated:YES];
    }
}
