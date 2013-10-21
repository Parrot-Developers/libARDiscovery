//
//  ARDiscovery.h
//  ARSDK 3
//
//  Created by Nicolas BRULEZ on 08/03/13.
//  Copyright (c) 2013 Parrot SA. All rights reserved.
//
#import <Foundation/Foundation.h>
#import <CoreBluetooth/CoreBluetooth.h>

#pragma mark Notifications

/**
 * Constant for devices services list updates notification
 * userInfo is a NSDictionnary with the following content:
 *  - key   : kARDiscoveryServicesList
 *  - value : NSArray of NSNetService
 */
#define kARDiscoveryNotificationServicesDevicesListUpdated @"kARDiscoveryNotificationServicesDevicesListUpdated"
#define kARDiscoveryServicesList @"kARDiscoveryServicesList"

/**
 * Constant for controller services list updates notification
 * userInfo is a NSDictionnary with the following content:
 *  - key   : kARDiscoveryServicesList
 *  - value : NSArray of NSNetService
 */
#define kARDiscoveryNotificationServicesControllersListUpdated @"kARDiscoveryNotificationServicesControllersListUpdated"

/**
 * Constant for publication notifications
 * userInfo is a NSDictionnary with the following content:
 *  - key   : kARDiscoveryServiceName
 *  - value : NSString with the name of the published service
 *            or @"" if no service is published
 */
#define kARDiscoveryNotificationServicePublished @"kARDiscoveryNotificationServicePublished"
#define kARDiscoveryServiceName @"kARDiscoveryServiceName"

/**
 * Constant for service resolution notifications
 * userInfo is a NSDictionnary with the following content:
 *  - key   : kARDiscoveryServiceResolved
 *  - value : NSNetService which was resolved
 */
#define kARDiscoveryNotificationServiceResolved @"kARDiscoveryNotificationServiceResolved"
#define kARDiscoveryServiceResolved @"kARDiscoveryServiceResolved"

#pragma mark ARDiscovery interface
@interface ARBLEService : NSObject
@property (nonatomic, strong) CBCentralManager *centralManager;
@property (nonatomic, strong) CBPeripheral *peripheral;
@end

@interface ARService : NSObject
@property (nonatomic, strong) NSString *name;
@property (nonatomic, strong) id service; /**< Can be NSNetService or ARBLEService */
@end

@interface ARDiscovery : NSObject

+ (ARDiscovery *)sharedInstance;

#pragma mark Start / Stop
/**
 * Starts ARDiscovery if it isntt discovering
 */
- (void)start;

/**
 * Stops ARDiscovery if it's discovering
 */
- (void)stop;

#pragma mark - Getters

/**
 * Get the current list of controllers services
 * Returns a NSArray of NSNetServices
 */
- (NSArray *)getCurrentListOfControllersServices;

/**
 * Get the current list of devices services
 * Returns a NSArray of NSNetServices or ARBLEServices
 */
- (NSArray *)getCurrentListOfDevicesServices;

/**
 * Get the name of the currently published service
 * Returns @"" if no service is published
 */
- (NSString *)getCurrentPublishedServiceName;

#pragma mark - Resolve

/**
 * Try to resolve the given service
 * Resolution is queued until all previous resolutions
 * are complete, or failed
 */
- (void)resolveService:(ARService *)aService;

/**
 * Convert the given service to resolved IP
 * You must to resolve service before to Convert it to IP
 */
- (NSString *)convertNSNetServiceToIp:(ARService *)service;

#pragma mark - Publication

/**
 * Publish a new device service with the given name
 * Calling this function will unpublish any previous service
 * If serviceName is not unique, it will be postfixed with a number
 *  --> "MyServiceName" will become "MyServiceName1", "MyServiceName2" ...
 */
- (void)publishDeviceServiceWithName:(NSString *)serviceName;

/**
 * Publish a new controller service with the given name
 * Calling this function will unpublish any previous service
 * If serviceName is not unique, it will be postfixed with a number
 *  --> "MyServiceName" will become "MyServiceName1", "MyServiceName2" ...
 */
- (void)publishControllerServiceWithName:(NSString *)serviceName;

/**
 * Stop publication of current service
 * Does nothing if no service is currently published
 */
- (void)unpublishService;

@end
