#include <jni.h>
#include <string.h>
#include <math.h>
#include "swephexp.h"
#include "sweph.h"
#include "swephlib.h"

JNIEXPORT jdoubleArray JNICALL
Java_dev_yantra_app_engine_SwissEphemeris_nativeLongitudes(
        JNIEnv *env,
        jobject thiz,
        jdouble julian_day_ut,
        jdouble latitude,
        jdouble longitude,
        jstring ephe_path,
        jint ayanamsa_mode) {
    (void) thiz;

    const char *path = (*env)->GetStringUTFChars(env, ephe_path, 0);
    swe_set_ephe_path((char *) path);
    swe_set_sid_mode((int32) ayanamsa_mode, 0, 0);

    double sun[6] = {0};
    double moon[6] = {0};
    double cusps[13] = {0};
    double ascmc[10] = {0};
    char serr[AS_MAXCH] = {0};
    int flags = SEFLG_SWIEPH | SEFLG_SIDEREAL | SEFLG_SPEED;
    int house_flags = SEFLG_SWIEPH | SEFLG_SIDEREAL;

    int sun_result = swe_calc_ut(julian_day_ut, SE_SUN, flags, sun, serr);
    int moon_result = swe_calc_ut(julian_day_ut, SE_MOON, flags, moon, serr);
    int house_result = swe_houses_ex(julian_day_ut, house_flags, latitude, longitude, 'P', cusps, ascmc);

    (*env)->ReleaseStringUTFChars(env, ephe_path, path);

    jdoubleArray result = (*env)->NewDoubleArray(env, 5);
    if (result == 0) {
        return 0;
    }

    double values[5];
    values[0] = sun_result < 0 ? -1.0 : sun[0];
    values[1] = moon_result < 0 ? -1.0 : moon[0];
    values[2] = (double) sun_result;
    values[3] = (double) moon_result;
    values[4] = house_result < 0 ? -1.0 : ascmc[0];
    (*env)->SetDoubleArrayRegion(env, result, 0, 5, values);
    return result;
}

/* All JNI entry points are serialized by SwissEphemeris.NativeBridge: Swiss
 * Ephemeris stores the ephemeris path and sidereal mode in shared native state. */
JNIEXPORT jdoubleArray JNICALL
Java_dev_yantra_app_engine_SwissEphemeris_nativeChart(
        JNIEnv *env, jobject thiz, jdouble jd, jstring ephe_path, jint mode) {
    (void) thiz;
    const char *path = (*env)->GetStringUTFChars(env, ephe_path, 0);
    if (!path) return NULL;
    swe_set_ephe_path((char *) path);
    swe_set_sid_mode(mode, 0, 0);
    (*env)->ReleaseStringUTFChars(env, ephe_path, path);
    double sun[6], moon[6], sid_sun[6], sid_moon[6], values[729];
    char error[AS_MAXCH] = {0};
    int flags = SEFLG_SWIEPH | SEFLG_J2000 | SEFLG_EQUATORIAL | SEFLG_NOABERR | SEFLG_NOGDEFL;
    if (swe_calc_ut(jd, SE_SUN, flags, sun, error) < 0 ||
        swe_calc_ut(jd, SE_MOON, flags, moon, error) < 0 ||
        swe_calc_ut(jd, SE_SUN, SEFLG_SWIEPH | SEFLG_SIDEREAL, sid_sun, error) < 0 ||
        swe_calc_ut(jd, SE_MOON, SEFLG_SWIEPH | SEFLG_SIDEREAL, sid_moon, error) < 0)
        return (*env)->NewDoubleArray(env, 0);
    values[0] = sun[0]; values[1] = sun[1];
    values[2] = moon[0]; values[3] = moon[1];
    double cosine = sin(sun[1] * DEGTORAD) * sin(moon[1] * DEGTORAD) +
        cos(sun[1] * DEGTORAD) * cos(moon[1] * DEGTORAD) * cos((sun[0] - moon[0]) * DEGTORAD);
    double distance = sqrt(sun[2]*sun[2] + moon[2]*moon[2] - 2*sun[2]*moon[2]*cosine);
    values[4] = (1 + (moon[2] - sun[2]*cosine) / distance) / 2;
    values[5] = sid_moon[0]; values[6] = sid_sun[0];
    double et = jd + swe_deltat_ex(jd, SEFLG_SWIEPH, error);
    double eps = swi_epsiln(et, 0);
    double ayanamsa = swe_get_ayanamsa_ut(jd);
    for (int i = 0; i <= 360; i++) {
        double lon = (i + ayanamsa) * DEGTORAD;
        double vector[3] = {cos(lon), sin(lon)*cos(eps), sin(lon)*sin(eps)};
        swi_precess(vector, et, 0, J_TO_J2000);
        double ra = atan2(vector[1], vector[0]) * RADTODEG;
        values[7 + 2*i] = ra < 0 ? ra + 360 : ra;
        values[8 + 2*i] = atan2(vector[2], hypot(vector[0], vector[1])) * RADTODEG;
    }
    jdoubleArray result = (*env)->NewDoubleArray(env, 729);
    if (result) (*env)->SetDoubleArrayRegion(env, result, 0, 729, values);
    return result;
}
