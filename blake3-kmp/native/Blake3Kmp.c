/*
 * Copyright (C) 2026 Akhsaul
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#include <jni.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include "blake3.h"

#if defined(__ANDROID__) || defined(ANDROID)
size_t strlen(const char *s) {
    const char *p = s;
    while (p && *p) p++;
    return (size_t)(p - s);
}
#endif

JNIEXPORT jlong JNICALL
Java_com_akhsaul_blake3_JniBlake3Kt_createHasher(JNIEnv* env, jclass clazz) {
    blake3_hasher* hasher = (blake3_hasher*) malloc(sizeof(blake3_hasher));
    if (hasher != NULL) {
        blake3_hasher_init(hasher);
    }
    return (jlong)(uintptr_t)hasher;
}

JNIEXPORT jlong JNICALL
Java_com_akhsaul_blake3_JniBlake3Kt_createKeyedHasher(JNIEnv* env, jclass clazz, jbyteArray keyArray) {
    if (keyArray == NULL) return 0;
    jsize keyLen = (*env)->GetArrayLength(env, keyArray);
    if (keyLen != BLAKE3_KEY_LEN) return 0;

    blake3_hasher* hasher = (blake3_hasher*) malloc(sizeof(blake3_hasher));
    if (hasher != NULL) {
        jbyte keyBuf[BLAKE3_KEY_LEN];
        (*env)->GetByteArrayRegion(env, keyArray, 0, BLAKE3_KEY_LEN, keyBuf);
        blake3_hasher_init_keyed(hasher, (const uint8_t*)keyBuf);
    }
    return (jlong)(uintptr_t)hasher;
}

JNIEXPORT void JNICALL
Java_com_akhsaul_blake3_JniBlake3Kt_hasherUpdate(JNIEnv* env, jclass clazz, jlong hasherPtr, jbyteArray input, jint offset, jint length) {
    if (hasherPtr == 0 || input == NULL || length <= 0) return;
    blake3_hasher* hasher = (blake3_hasher*)(uintptr_t)hasherPtr;

    jbyte* buffer = (jbyte*)(*env)->GetPrimitiveArrayCritical(env, input, NULL);
    if (buffer != NULL) {
        blake3_hasher_update(hasher, buffer + offset, (size_t)length);
        (*env)->ReleasePrimitiveArrayCritical(env, input, buffer, JNI_ABORT);
    }
}

JNIEXPORT void JNICALL
Java_com_akhsaul_blake3_JniBlake3Kt_hasherFinalize(JNIEnv* env, jclass clazz, jlong hasherPtr, jbyteArray output, jint offset, jint length) {
    if (hasherPtr == 0 || output == NULL || length <= 0) return;
    blake3_hasher* hasher = (blake3_hasher*)(uintptr_t)hasherPtr;

    jbyte* buffer = (jbyte*)(*env)->GetPrimitiveArrayCritical(env, output, NULL);
    if (buffer != NULL) {
        blake3_hasher_finalize(hasher, (uint8_t*)(buffer + offset), (size_t)length);
        (*env)->ReleasePrimitiveArrayCritical(env, output, buffer, 0);
    }
}

JNIEXPORT void JNICALL
Java_com_akhsaul_blake3_JniBlake3Kt_hasherReset(JNIEnv* env, jclass clazz, jlong hasherPtr) {
    if (hasherPtr == 0) return;
    blake3_hasher* hasher = (blake3_hasher*)(uintptr_t)hasherPtr;
    blake3_hasher_reset(hasher);
}

JNIEXPORT void JNICALL
Java_com_akhsaul_blake3_JniBlake3Kt_freeHasher(JNIEnv* env, jclass clazz, jlong hasherPtr) {
    if (hasherPtr == 0) return;
    blake3_hasher* hasher = (blake3_hasher*)(uintptr_t)hasherPtr;
    free(hasher);
}

JNIEXPORT void JNICALL
Java_com_akhsaul_blake3_JniBlake3Kt_hash(JNIEnv* env, jclass clazz, jbyteArray input, jint offset, jint length, jbyteArray output, jint outLen) {
    if (output == NULL || outLen <= 0) return;

    blake3_hasher hasher;
    blake3_hasher_init(&hasher);

    if (input != NULL && length > 0) {
        jbyte* inBuf = (jbyte*)(*env)->GetPrimitiveArrayCritical(env, input, NULL);
        if (inBuf != NULL) {
            blake3_hasher_update(&hasher, inBuf + offset, (size_t)length);
            (*env)->ReleasePrimitiveArrayCritical(env, input, inBuf, JNI_ABORT);
        }
    }

    jbyte* outBuf = (jbyte*)(*env)->GetPrimitiveArrayCritical(env, output, NULL);
    if (outBuf != NULL) {
        blake3_hasher_finalize(&hasher, (uint8_t*)outBuf, (size_t)outLen);
        (*env)->ReleasePrimitiveArrayCritical(env, output, outBuf, 0);
    }
}

JNIEXPORT void JNICALL
Java_com_akhsaul_blake3_JniBlake3Kt_keyedHash(JNIEnv* env, jclass clazz, jbyteArray keyArray, jbyteArray input, jint offset, jint length, jbyteArray output, jint outLen) {
    if (keyArray == NULL || output == NULL || outLen <= 0) return;
    if ((*env)->GetArrayLength(env, keyArray) != BLAKE3_KEY_LEN) return;

    jbyte keyBuf[BLAKE3_KEY_LEN];
    (*env)->GetByteArrayRegion(env, keyArray, 0, BLAKE3_KEY_LEN, keyBuf);

    blake3_hasher hasher;
    blake3_hasher_init_keyed(&hasher, (const uint8_t*)keyBuf);

    if (input != NULL && length > 0) {
        jbyte* inBuf = (jbyte*)(*env)->GetPrimitiveArrayCritical(env, input, NULL);
        if (inBuf != NULL) {
            blake3_hasher_update(&hasher, inBuf + offset, (size_t)length);
            (*env)->ReleasePrimitiveArrayCritical(env, input, inBuf, JNI_ABORT);
        }
    }

    jbyte* outBuf = (jbyte*)(*env)->GetPrimitiveArrayCritical(env, output, NULL);
    if (outBuf != NULL) {
        blake3_hasher_finalize(&hasher, (uint8_t*)outBuf, (size_t)outLen);
        (*env)->ReleasePrimitiveArrayCritical(env, output, outBuf, 0);
    }
}
