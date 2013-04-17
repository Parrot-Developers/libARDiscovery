//
//  ARDiscovery.m
//  ARSDK 3
//
//  Created by Nicolas BRULEZ on 08/03/13.
//  Copyright (c) 2013 Parrot SA. All rights reserved.
//

#import "ARDiscovery.h"
#import <netdb.h>

#define kServiceDeviceType @"_arsdk-mk3._udp."
#define kServiceControllerType @"_arsdk-ff3._udp."
#define kServiceDomain @"local."

#define CHECK_VALID(DEFAULT_RETURN_VALUE)       \
    do                                          \
    {                                           \
        if (! self.valid)                       \
        {                                       \
            return DEFAULT_RETURN_VALUE;        \
        }                                       \
    } while (0)

#pragma mark Private part

@interface ARDiscovery () <NSNetServiceBrowserDelegate, NSNetServiceDelegate>

#pragma mark - Controller/Devices Services list
@property (strong, nonatomic) NSMutableArray *controllersServicesList;
@property (strong, nonatomic) NSMutableArray *devicesServicesList;

#pragma mark - Current published service
@property (strong, nonatomic) NSNetService *currentPublishedService;
@property (strong, nonatomic) NSNetService *tryPublishService;

#pragma mark - Services browser / resolution
@property (strong, nonatomic) NSNetService *currentResolutionService;
@property (strong, nonatomic) NSString *currentResolutionServiceIp;
@property (strong, nonatomic) NSNetServiceBrowser *controllersServiceBrowser;
@property (strong, nonatomic) NSNetServiceBrowser *devicesServiceBrowser;

#pragma mark - Object properly created
@property (nonatomic) BOOL valid;

@end

#pragma mark Implementation

@implementation ARDiscovery

@synthesize controllersServicesList;
@synthesize devicesServicesList;
@synthesize currentPublishedService;
@synthesize tryPublishService;
@synthesize currentResolutionService;
@synthesize currentResolutionServiceIp;
@synthesize controllersServiceBrowser;
@synthesize devicesServiceBrowser;
@synthesize valid;


#pragma mark - Init

+ (ARDiscovery *)sharedInstance
{
    static ARDiscovery *sharedInstance = nil;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        sharedInstance = [[ARDiscovery alloc] init];
        
        /**
         * Services list init
         */
        sharedInstance.controllersServicesList = [[NSMutableArray alloc] initWithCapacity:10];
        sharedInstance.devicesServicesList = [[NSMutableArray alloc] initWithCapacity:10];
        
        /**
         * Current published service init
         */
        sharedInstance.currentPublishedService = nil;
        sharedInstance.tryPublishService = nil;
        
        /**
         * Services browser / resolution init
         */
        sharedInstance.controllersServiceBrowser = [[NSNetServiceBrowser alloc] init];
        [sharedInstance.controllersServiceBrowser setDelegate:sharedInstance];
        sharedInstance.devicesServiceBrowser = [[NSNetServiceBrowser alloc] init];
        [sharedInstance.devicesServiceBrowser setDelegate:sharedInstance];
        sharedInstance.currentResolutionService = nil;
        
        /**
         * Start NSNetServiceBrowser
         */
        [sharedInstance.controllersServiceBrowser searchForServicesOfType:kServiceControllerType inDomain:kServiceDomain];
        [sharedInstance.devicesServiceBrowser searchForServicesOfType:kServiceDeviceType inDomain:kServiceDomain];
        
        /**
         * Creation was done as a shared instance
         */
        sharedInstance.valid = YES;
    });
    return sharedInstance;
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
        self.tryPublishService = [[NSNetService alloc] initWithDomain:kServiceDomain type:kServiceDeviceType name:uniqueName port:9];
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
        self.tryPublishService = [[NSNetService alloc] initWithDomain:kServiceDomain type:kServiceControllerType name:uniqueName port:9];
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
        if ([[aNetService type] isEqual:kServiceDeviceType])
        {
            [self.devicesServicesList addObject:aNetService];
            if (!moreComing)
            {
                [self sendDevicesListUpdateNotification];
            }
        }
        else if ([[aNetService type] isEqual:kServiceControllerType])
        {
            [self.controllersServicesList addObject:aNetService];
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
        if ([[aNetService type] isEqual:kServiceDeviceType])
        {
            [self.devicesServicesList removeObject:aNetService];
            if (!moreComing)
            {
                [self sendDevicesListUpdateNotification];
            }
        }
        else if ([[aNetService type] isEqual:kServiceControllerType])
        {
            [self.controllersServicesList removeObject:aNetService];
            if (!moreComing)
            {
                [self sendControllersListUpdateNotification];
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

- (void)netService:(NSNetService *)sender didNotPublish:(NSDictionary *)errorDict
{
    @synchronized (self)
    {
        self.currentPublishedService = nil;
        [self sendPublishNotification];
    }
}

- (void)netServiceDidPublish:(NSNetService *)sender
{
    @synchronized (self)
    {
        self.currentPublishedService = sender;
        [self sendPublishNotification];
    }
}

- (void)netService:(NSNetService *)sender didNotResolve:(NSDictionary *)errorDict
{
    @synchronized (self)
    {
        self.currentResolutionServiceIp = nil;
        [self sendResolveNotification];
    }
}

- (void)netServiceDidResolveAddress:(NSNetService *)sender
{
    @synchronized (self)
    {
        self.currentResolutionServiceIp = nil;
        struct hostent *hostInfos = gethostbyname ([[sender hostName] UTF8String]);
        if ((hostInfos != NULL) &&
            (hostInfos->h_length == 4))
        {
            int cnt = 0;
            char *addr;
            while ((addr = hostInfos->h_addr_list[cnt++]) != NULL)
            {
                //NSLog (@"ADDR %d : %u.%u.%u.%u", cnt, addr[0] & 0x0ff, addr[1] & 0x0ff, addr[2] & 0x0ff, addr[3] & 0x0ff);
                self.currentResolutionServiceIp = [NSString stringWithFormat:@"%u.%u.%u.%u", addr[0] & 0x0ff, addr[1] & 0x0ff, addr[2] & 0x0ff, addr[3] & 0x0ff];
            }
        }
        [self sendResolveNotification];
    }
}

#pragma mark - Notification senders

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
    NSDictionary *userInfos = @{kARDiscoveryServiceResolved: self.currentResolutionService, kARDiscoveryServiceIP : self.currentResolutionServiceIp};
    [[NSNotificationCenter defaultCenter] postNotificationName:kARDiscoveryNotificationServiceResolved object:self userInfo:userInfos];
}

@end
