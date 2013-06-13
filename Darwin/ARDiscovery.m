//
//  ARDiscovery.m
//  ARSDK 3
//
//  Created by Nicolas BRULEZ on 08/03/13.
//  Copyright (c) 2013 Parrot SA. All rights reserved.
//
#include <arpa/inet.h>
#import "ARDiscovery.h"
#import <netdb.h>
#define kServiceNetDeviceType @"_arsdk-mk3._udp."
#define kServiceNetControllerType @"_arsdk-ff3._udp."
#define kServiceNetDomain @"local."

#define kServiceBLEDeviceType @"Mykonos_BLE" // TO DO

#define CHECK_VALID(DEFAULT_RETURN_VALUE)       \
    do                                          \
    {                                           \
        if (! self.valid)                       \
        {                                       \
            return DEFAULT_RETURN_VALUE;        \
        }                                       \
    } while (0)

#pragma mark ARBLEService implmentation
@implementation ARBLEService

@end

@implementation ARService

@end

#pragma mark Private part
@interface ARDiscovery () <NSNetServiceBrowserDelegate, NSNetServiceDelegate, CBCentralManagerDelegate>

#pragma mark - Controller/Devices Services list
@property (strong, nonatomic) NSMutableArray *controllersServicesList;
@property (strong, nonatomic) NSMutableArray *devicesServicesList;

#pragma mark - Current published service
@property (strong, nonatomic) NSNetService *currentPublishedService;
@property (strong, nonatomic) NSNetService *tryPublishService;

#pragma mark - Services browser / resolution
@property (strong, nonatomic) NSNetService *currentResolutionService;
@property (strong, nonatomic) NSNetServiceBrowser *controllersServiceBrowser;
@property (strong, nonatomic) NSNetServiceBrowser *devicesServiceBrowser;

#pragma mark - Services CoreBluetooth
@property (strong, nonatomic) CBCentralManager *centralManager;

#pragma mark - Object properly created
@property (nonatomic) BOOL valid;

#pragma mark - Object properly created
@property (nonatomic) BOOL isDiscovering;

@end

#pragma mark Implementation

@implementation ARDiscovery

@synthesize controllersServicesList;
@synthesize devicesServicesList;
@synthesize currentPublishedService;
@synthesize tryPublishService;
@synthesize currentResolutionService;
@synthesize controllersServiceBrowser;
@synthesize devicesServiceBrowser;
@synthesize centralManager;
@synthesize valid;
@synthesize isDiscovering;

#pragma mark - Init
+ (ARDiscovery *)sharedInstance
{
    static ARDiscovery *_sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        _sharedInstance = [[ARDiscovery alloc] init];
        
        /**
         * Services list init
         */
        _sharedInstance.controllersServicesList = [[NSMutableArray alloc] initWithCapacity:10];
        _sharedInstance.devicesServicesList = [[NSMutableArray alloc] initWithCapacity:10];
        
        /**
         * Current published service init
         */
        _sharedInstance.currentPublishedService = nil;
        _sharedInstance.tryPublishService = nil;
        
        /**
         * Services browser / resolution init
         */
        _sharedInstance.controllersServiceBrowser = [[NSNetServiceBrowser alloc] init];
        [_sharedInstance.controllersServiceBrowser setDelegate:_sharedInstance];
        _sharedInstance.devicesServiceBrowser = [[NSNetServiceBrowser alloc] init];
        [_sharedInstance.devicesServiceBrowser setDelegate:_sharedInstance];
        _sharedInstance.currentResolutionService = nil;
        
        /**
         * Creation was done as a shared instance
         */
        _sharedInstance.valid = YES;
        
        /**
         * Discover is not in progress
         */
        _sharedInstance.isDiscovering = NO;
    });
    
    return _sharedInstance;
}

#pragma mark - Getters

- (NSArray *)getCurrentListOfDevicesServices
{
    NSArray *array = nil;
    CHECK_VALID(array);
    @synchronized (self)
    {
        array = [self.devicesServicesList copy];
    }
    return array;
}

- (NSArray *)getCurrentListOfControllersServices
{
    NSArray *array = nil;
    CHECK_VALID(array);
    @synchronized (self)
    {
        array = [self.controllersServicesList copy];
    }
    return array;
}

- (NSString *)getCurrentPublishedServiceName
{
    NSString *name = nil;
    CHECK_VALID(name);
    @synchronized (self)
    {
        name = [[self.currentPublishedService name] copy];
    }
    return name;
}

#pragma mark - Discovery

- (void)resolveService:(NSNetService *)aService
{
    CHECK_VALID();
    @synchronized (self)
    {
        [self.currentResolutionService stop];
        self.currentResolutionService = aService;
        [self.currentResolutionService setDelegate:self];
    
        [self.currentResolutionService resolveWithTimeout:2.0];
    }
}

- (void)start
{
    if(!isDiscovering)
    {
        /**
         * Start NSNetServiceBrowser
         */
        [controllersServiceBrowser searchForServicesOfType:kServiceNetControllerType inDomain:kServiceNetDomain];
        [devicesServiceBrowser searchForServicesOfType:kServiceNetDeviceType inDomain:kServiceNetDomain];
        
        /**
         * Start CoreBluetooth discovery
         */
        centralManager = [[CBCentralManager alloc] initWithDelegate:self queue:nil];
        isDiscovering = YES;
    }
}

- (void)stop
{
    if(isDiscovering)
    {
        /**
         * Stop NSNetServiceBrowser
         */
        [controllersServiceBrowser stop];
        [devicesServiceBrowser stop];
        
        /**
         * Stop CoreBluetooth discovery
         */
        [centralManager stopScan];
        centralManager = nil;
        
        isDiscovering = NO;
    }
}

- (NSString *)convertNSNetServiceToIp:(NSNetService *)service
{
    NSString *name = nil;
    NSData *address = nil;
    struct sockaddr_in *socketAddress = nil;
    NSString *ipString = nil;
    int port;
    name = [service name];
    address = [[service addresses] objectAtIndex: 0];
    socketAddress = (struct sockaddr_in *) [address bytes];
    ipString = [NSString stringWithFormat: @"%s",inet_ntoa(socketAddress->sin_addr)];
    port = socketAddress->sin_port;
    // This will print the IP and port for you to connect to.
    NSLog(@"%@", [NSString stringWithFormat:@"Resolved:%@-->%@:%u\n", [service hostName], ipString, port]);
    
    return ipString;
}

#pragma mark - Publication
- (NSString *)uniqueNameFromServiceName:(NSString *)sname isController:(BOOL)isController
{
    NSString *rname = [sname copy];
    
    int addCount = 1;
    
    NSArray *servicesCopy;
    if (isController)
    {
        servicesCopy = [self getCurrentListOfControllersServices];
    }
    else
    {
        servicesCopy = [self getCurrentListOfDevicesServices];
    }
    BOOL rnameIsUnique = YES;
    do {
        rnameIsUnique = YES;
        for (NSNetService *ns in servicesCopy) {
            if ([rname isEqualToString:[ns name]])
            {
                rnameIsUnique = NO;
                break;
            }
        }
        if (! rnameIsUnique)
        {
            rname = [sname stringByAppendingFormat:@"%d", addCount++];
        }
    } while (! rnameIsUnique);
    return rname;
}

- (void)publishDeviceServiceWithName:(NSString *)serviceName
{
    CHECK_VALID();
    @synchronized (self)
    {
        NSString *uniqueName = [self uniqueNameFromServiceName:serviceName isController:NO];
        [self.tryPublishService stop];
        self.tryPublishService = [[NSNetService alloc] initWithDomain:kServiceNetDomain type:kServiceNetDeviceType name:uniqueName port:9];
        [self.tryPublishService setDelegate:self];
        [self.tryPublishService publish];
    }
}

- (void)publishControllerServiceWithName:(NSString *)serviceName
{
    CHECK_VALID();
    @synchronized (self)
    {
        NSString *uniqueName = [self uniqueNameFromServiceName:serviceName isController:YES];
        [self.tryPublishService stop];
        self.tryPublishService = [[NSNetService alloc] initWithDomain:kServiceNetDomain type:kServiceNetControllerType name:uniqueName port:9];
        [self.tryPublishService setDelegate:self];
        [self.tryPublishService publish];
    }
}

- (void)unpublishService
{
    CHECK_VALID();
    @synchronized (self)
    {
        [self.tryPublishService stop];
        self.tryPublishService = nil;
        self.currentPublishedService = nil;
        [self sendPublishNotification];
    }
}

#pragma mark - NSNetServiceBrowser Delegate
- (void)netServiceBrowser:(NSNetServiceBrowser *)aNetServiceBrowser didFindService:(NSNetService *)aNetService moreComing:(BOOL)moreComing
{
    @synchronized (self)
    {
        if ([[aNetService type] isEqual:kServiceNetDeviceType])
        {
            ARService *aService = [[ARService alloc] init];
            aService.name = [aNetService name];
            aService.service = aNetService;
            [self.devicesServicesList addObject:aService];
            if (!moreComing)
            {
                [self sendDevicesListUpdateNotification];
            }
        }
        else if ([[aNetService type] isEqual:kServiceNetControllerType])
        {
            ARService *aService = [[ARService alloc] init];
            aService.name = [aNetService name];
            aService.service = aNetService;
            [self.controllersServicesList addObject:aService];
            if (!moreComing)
            {
                [self sendControllersListUpdateNotification];
            }
        }
        else
        {
#ifdef DEBUG
            NSLog (@"Found an unknown service : %@", aNetService);
#endif
        }
    }
}

- (void)netServiceBrowser:(NSNetServiceBrowser *)aNetServiceBrowser didRemoveService:(NSNetService *)aNetService moreComing:(BOOL)moreComing
{
    @synchronized (self)
    {
        if ([[aNetService type] isEqual:kServiceNetDeviceType])
        {
            NSEnumerator *enumerator = [self.devicesServicesList objectEnumerator];
            ARService *aService = nil;
            BOOL found = NO;
            while(!found && ((aService = [enumerator nextObject]) != nil))
            {
                NSLog(@"%@, %@", [aService name], [aNetService name]);
                if([[aService name] isEqualToString:[aNetService name]])
                {
                    [self.devicesServicesList removeObject:aService];
                    found = YES;
                    if (!moreComing)
                    {
                        [self sendDevicesListUpdateNotification];
                    }
                }
            }
        }
        else if ([[aNetService type] isEqual:kServiceNetControllerType])
        {
            NSEnumerator *enumerator = [self.controllersServicesList objectEnumerator];
            ARService *aService = nil;
            BOOL found = NO;
            while(!found && ((aService = [enumerator nextObject]) != nil))
            {
                if([[aService name] isEqualToString:[aNetService name]])
                {
                    [self.controllersServicesList removeObject:aService];
                    found = YES;
                    if (!moreComing)
                    {
                        [self sendControllersListUpdateNotification];
                    }
                }
            }
        }
        else
        {
#ifdef DEBUG
            NSLog (@"Removed an unknown service : %@", aNetService);
#endif
        }
    }
}

#pragma mark - NSNetService Delegate

- (void)netService:(NSNetService *)service didNotPublish:(NSDictionary *)errorDict
{
    @synchronized (self)
    {
        self.currentPublishedService = nil;
        [self sendPublishNotification];
    }
}

- (void)netServiceDidPublish:(NSNetService *)service
{
    @synchronized (self)
    {
        self.currentPublishedService = service;
        [self sendPublishNotification];
    }
}

- (void)netService:(NSNetService *)service didNotResolve:(NSDictionary *)errorDict
{
    @synchronized (self)
    {
        [self sendResolveNotification];
    }
}

- (void)netServiceDidResolveAddress:(NSNetService *)service
{
    @synchronized (self)
    {
        [self sendResolveNotification];
    }
}

#pragma mark - CBCentralManagerDelegate methods
- (void)centralManagerDidUpdateState:(CBCentralManager *)central
{
    switch(central.state)
    {
        case CBCentralManagerStatePoweredOn:
            NSLog(@"CBCentralManagerStatePoweredOn");
            // Start scanning peripherals
            [central scanForPeripheralsWithServices:nil options:0];
            break;
            
        case CBCentralManagerStateResetting:
            NSLog(@"CBCentralManagerStateResetting");
            break;
            
        case CBCentralManagerStateUnsupported:
            NSLog(@"CBCentralManagerStateUnsupported");
            break;
            
        case CBCentralManagerStateUnauthorized:
            NSLog(@"CBCentralManagerStateUnauthorized");
            break;
            
        case CBCentralManagerStatePoweredOff:
            NSLog(@"CBCentralManagerStatePoweredOff");
            break;
            
        default:
        case CBCentralManagerStateUnknown:
            NSLog(@"CBCentralManagerStateUnknown");
            break;
    }
}

- (void)centralManager:(CBCentralManager *)central didDiscoverPeripheral:(CBPeripheral *)peripheral advertisementData:(NSDictionary *)advertisementData RSSI:(NSNumber *)RSSI
{
    NSLog(@"Scanning %@ => advertisement data : %@", [peripheral name], advertisementData);
    
    if([peripheral name] != nil)
    {
        if ([peripheral.name hasPrefix:kServiceBLEDeviceType])
        {
            ARBLEService *service = [[ARBLEService alloc] init];
            service.centralManager = central;
            service.peripheral = peripheral;
            ARService *aService = [[ARService alloc] init];
            aService.name = [service.peripheral name];
            aService.service = service;
            [self.devicesServicesList addObject:aService];
            [self sendDevicesListUpdateNotification];
        }
    }
}

#pragma mark - Notification sender
- (void)sendPublishNotification
{
    NSDictionary *userInfos = @{kARDiscoveryServiceName: [self getCurrentPublishedServiceName]};
    [[NSNotificationCenter defaultCenter] postNotificationName:kARDiscoveryNotificationServicePublished object:self userInfo:userInfos];
}

- (void)sendDevicesListUpdateNotification
{
    NSDictionary *userInfos = @{kARDiscoveryServicesList: [self getCurrentListOfDevicesServices]};
    [[NSNotificationCenter defaultCenter] postNotificationName:kARDiscoveryNotificationServicesDevicesListUpdated object:self userInfo:userInfos];
}

- (void)sendControllersListUpdateNotification
{
    NSDictionary *userInfos = @{kARDiscoveryServicesList: [self getCurrentListOfControllersServices]};
    [[NSNotificationCenter defaultCenter] postNotificationName:kARDiscoveryNotificationServicesControllersListUpdated object:self userInfo:userInfos];
}

- (void)sendResolveNotification
{
    NSDictionary *userInfos = @{kARDiscoveryServiceResolved: self.currentResolutionService};
    [[NSNotificationCenter defaultCenter] postNotificationName:kARDiscoveryNotificationServiceResolved object:self userInfo:userInfos];
}

@end
