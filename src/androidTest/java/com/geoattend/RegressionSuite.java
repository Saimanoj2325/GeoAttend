package com.geoattend;

import com.geoattend.security.SecurityLogicTest;
import com.geoattend.security.MockLocationTest;
import com.geoattend.performance.StartupPerformanceTest;
import com.geoattend.attendance.OfflineAttendanceTest;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
        AppFlowTest.class,
        LoginActivityTest.class,
        SmokeTest.class,
        EdgeCaseTest.class,
        SecurityLogicTest.class,
        MockLocationTest.class,
        StartupPerformanceTest.class,
        OfflineAttendanceTest.class
})
public class RegressionSuite {
    // This class remains empty, used only as a holder for the above annotations
}
