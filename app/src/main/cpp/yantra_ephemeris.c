#include <jni.h>
#include <string.h>
#include "swephexp.h"

JNIEXPORT jdoubleArray JNICALL
Java_dev_yantra_app_engine_SwissEphemeris_nativeLongitudes(
        JNIEnv *env,
        jobject thiz,
        jdouble julian_day_ut,
        jstring ephe_path) {
    (void) thiz;

    const char *path = (*env)->GetStringUTFChars(env, ephe_path, 0);
    swe_set_ephe_path((char *) path);
    swe_set_sid_mode(SE_SIDM_LAHIRI, 0, 0);

    double sun[6] = {0};
    double moon[6] = {0};
    char serr[AS_MAXCH] = {0};
    int flags = SEFLG_SWIEPH | SEFLG_SIDEREAL | SEFLG_SPEED;

    int sun_result = swe_calc_ut(julian_day_ut, SE_SUN, flags, sun, serr);
    int moon_result = swe_calc_ut(julian_day_ut, SE_MOON, flags, moon, serr);

    (*env)->ReleaseStringUTFChars(env, ephe_path, path);

    jdoubleArray result = (*env)->NewDoubleArray(env, 4);
    if (result == 0) {
        return 0;
    }

    double values[4];
    values[0] = sun_result < 0 ? -1.0 : sun[0];
    values[1] = moon_result < 0 ? -1.0 : moon[0];
    values[2] = (double) sun_result;
    values[3] = (double) moon_result;
    (*env)->SetDoubleArrayRegion(env, result, 0, 4, values);
    return result;
}
