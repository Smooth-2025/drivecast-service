package com.smooth.drivecast_service.driving.util;

import com.smooth.drivecast_service.driving.constants.DrivingVicinityPolicy;
import com.smooth.drivecast_service.global.util.KoreanTimeUtil;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class LocationWindowKeyGenerator {

    public static List<String> generateWindowKeys(Instant refUtc, int windowSec) {
        List<String> keys = new ArrayList<>();

        Instant futureTime = refUtc.plusSeconds(1);
        String futureKey = KoreanTimeUtil.toLocationKey(futureTime);
        keys.add(futureKey);

        for(int i = 0; i < windowSec; i++) {
            Instant targetTime = refUtc.minusSeconds(i);
            String locationKey = KoreanTimeUtil.toLocationKey(targetTime);
            keys.add(locationKey);
        }

        return keys;
    }

    public static List<String> generateDefaultWindowKeys(Instant refUtc) {
      return generateWindowKeys(refUtc, DrivingVicinityPolicy.WINDOW_SECONDS);
    }

}
