//
//  FBRangingStatusStreamHandler.m
//  flutter_beacon
//
//  Created by Muneer on 21/08/23.
//

#import "FBRangingStatusStreamHandler.h"
#import <FlutterBeaconPlugin.h>

@implementation FBRangingStatusStreamHandler

- (instancetype) initWithFlutterBeaconPlugin:(FlutterBeaconPlugin*) instance {
    if (self = [super init]) {
        _instance = instance;
    }
    
    return self;
}

///------------------------------------------------------------
#pragma mark - Flutter Stream Handler
///------------------------------------------------------------

- (FlutterError * _Nullable)onCancelWithArguments:(id _Nullable)arguments {
    if (self.instance) {
        [self.instance stopRangingStatusTimer];
    }
    return nil;
}

- (FlutterError * _Nullable)onListenWithArguments:(id _Nullable)arguments eventSink:(nonnull FlutterEventSink)events {
    if (self.instance) {
        self.instance.flutterEventSinkRangingStatus = events;
    }
    return nil;
}

@end
