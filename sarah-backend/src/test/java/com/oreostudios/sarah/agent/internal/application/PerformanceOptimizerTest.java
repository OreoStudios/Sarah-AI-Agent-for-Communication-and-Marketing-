package com.oreostudios.sarah.agent.internal.application;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class PerformanceOptimizerTest {
 @Test void refusesSparseData(){assertEquals(1.0,PerformanceOptimizer.score("discord",100,90,1).weight());}
 @Test void capsOptimizationAndValidatesObservations(){assertEquals(2.0,PerformanceOptimizer.score("discord",1000,200,4).weight());assertEquals(0.5,PerformanceOptimizer.score("discord",10000,0,4).weight());assertFalse(PerformanceOptimizer.valid(100,4,5));assertTrue(PerformanceOptimizer.valid(100,4,2));}
}
