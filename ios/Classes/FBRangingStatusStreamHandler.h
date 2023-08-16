//
//  FBRangingStatusStreamHandler.h
//  flutter_beacon
//
//  Created by Muneer on 21/08/23.
//

#import <Foundation/Foundation.h>
#import <Flutter/Flutter.h>

NS_ASSUME_NONNULL_BEGIN

@class FlutterBeaconPlugin;
@interface FBRangingStatusStreamHandler : NSObject<FlutterStreamHandler>

@property (strong, nonatomic) FlutterBeaconPlugin* instance;

- (instancetype) initWithFlutterBeaconPlugin:(FlutterBeaconPlugin*) instance;

@end

NS_ASSUME_NONNULL_END
