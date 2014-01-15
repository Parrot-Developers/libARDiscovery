/**
 * @file ARDISCOVERY_JNI_Discovery.c
 * @brief libARDiscovery JNI dicovery c file.
 **/

/*****************************************
 * 
 *             include file :
 *
 *****************************************/

#include <jni.h>
#include <stdlib.h>

#include <libARSAL/ARSAL_Print.h>

#include <libARDiscovery/ARDISCOVERY_Discovery.h>


/*****************************************
 *
 *             define :
 *
 *****************************************/

#define ARDISCOVERY_JNIDISCOVERY_TAG "JNIDiscovery"

/*****************************************
 *
 *             implementation :
 *
 *****************************************/

 /**
 * @brief Converts a product enumerator in product ID
 * This function is the only one knowing the correspondance
 * between the enumerator and the products' IDs.
 * @param env reference to the java environment
 * @param thizz reference to the object calling this function
 * @param product The product's enumerator
 * @return The corresponding product ID
 */
JNIEXPORT jint JNICALL
Java_com_parrot_arsdk_ardiscovery_ARDiscoveryService_nativeGetProductID (JNIEnv *env, jclass thizz, int product)
{
    return ARDISCOVERY_getProductID (product);
}

/**
 * @brief get ARDISCOVERY_SERVICE_NET_DEVICE_FORMAT
 * @return value of ARDISCOVERY_SERVICE_NET_DEVICE_FORMAT
 */
 JNIEXPORT jstring JNICALL
Java_com_parrot_arsdk_ardiscovery_ARDiscoveryService_nativeGetDefineNetDeviceFormat (JNIEnv *env, jclass class)
{
    return  (*env)->NewStringUTF(env, ARDISCOVERY_SERVICE_NET_DEVICE_FORMAT);
}
