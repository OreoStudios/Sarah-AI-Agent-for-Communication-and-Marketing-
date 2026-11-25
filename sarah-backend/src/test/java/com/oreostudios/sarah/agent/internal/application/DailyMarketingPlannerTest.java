package com.oreostudios.sarah.agent.internal.application;
import com.oreostudios.sarah.shared.Platform;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class DailyMarketingPlannerTest {
 @Test void selectsOnlyAllowedChannelsAndFavorsMeasuredPerformance(){var selection=DailyMarketingPlanner.select(Set.of(Platform.DISCORD,Platform.LINKEDIN),Map.of("linkedin",2.0,"reddit",3.0),1,5);assertEquals(List.of(Platform.LINKEDIN),selection);}
 @Test void boundsSelection(){assertEquals(1,DailyMarketingPlanner.select(Set.of(Platform.FACEBOOK,Platform.DISCORD),Map.of(),1,8).size());}
}
