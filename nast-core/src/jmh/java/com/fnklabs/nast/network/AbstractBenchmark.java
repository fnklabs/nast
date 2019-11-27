package com.fnklabs.nast.network;

import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

@Threads(value = 4)
@Fork(value = 1, jvmArgs = {
        "-server",
        "-Xms512m",
        "-Xmx2G",
        "-XX:NewSize=512m",
        "-XX:SurvivorRatio=6",
        "-XX:+AlwaysPreTouch",
        "-XX:+UseG1GC",
        "-XX:MaxGCPauseMillis=2000",
        "-XX:GCTimeRatio=4",
        "-XX:InitiatingHeapOccupancyPercent=30",
        "-XX:G1HeapRegionSize=8M",
        "-XX:ConcGCThreads=8",
        "-XX:G1HeapWastePercent=10",
        "-XX:+UseTLAB",
        "-XX:+ScavengeBeforeFullGC",
        "-XX:+DisableExplicitGC",
})
@Warmup(iterations = 5, timeUnit = TimeUnit.MILLISECONDS)
public class AbstractBenchmark  {
}
