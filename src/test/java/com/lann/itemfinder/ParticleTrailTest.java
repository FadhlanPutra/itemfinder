package com.lann.itemfinder;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

public class ParticleTrailTest {

    @BeforeAll
    static void beforeAll() {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
    }

    @Test
    void everyOptionMapsToDistinctParticle() {
        Set<SimpleParticleType> seen = new HashSet<>();
        for (String option : ParticleTrail.PARTICLE_OPTIONS) {
            SimpleParticleType particle = ParticleTrail.getParticle(option);
            Assertions.assertNotNull(particle, "Option '" + option + "' produced null particle");

            boolean isNew = seen.add(particle);
            if (!option.equals("FLAME")) {
                Assertions.assertTrue(isNew,
                    "Option '" + option + "' produced the SAME particle as another already processed option " +
                    "-- likely this case does not match in the switch and silently fell through to default (FLAME). " +
                    "Check if the ParticleTypes constant for '" + option + "' still exists with the same name in this Minecraft version.");
            }
        }
    }

    @Test
    void specificMappingsAreCorrect() {
        Assertions.assertEquals(ParticleTypes.FLAME, ParticleTrail.getParticle("FLAME"));
        Assertions.assertEquals(ParticleTypes.ENCHANT, ParticleTrail.getParticle("ENCHANT"));
        Assertions.assertEquals(ParticleTypes.END_ROD, ParticleTrail.getParticle("END ROD"));
        Assertions.assertEquals(ParticleTypes.SOUL_FIRE_FLAME, ParticleTrail.getParticle("SOUL FIRE FLAME"));
        Assertions.assertEquals(ParticleTypes.WITCH, ParticleTrail.getParticle("WITCH"));
        Assertions.assertEquals(ParticleTypes.DRAGON_BREATH, ParticleTrail.getParticle("DRAGON BREATH"));
        Assertions.assertEquals(ParticleTypes.PORTAL, ParticleTrail.getParticle("PORTAL"));
        Assertions.assertEquals(ParticleTypes.HAPPY_VILLAGER, ParticleTrail.getParticle("HAPPY VILLAGER"));
    }

    @Test
    void unknownNameFallsBackToFlameWithoutCrashing() {
        Assertions.assertEquals(ParticleTypes.FLAME, ParticleTrail.getParticle("NOT_A_REAL_OPTION"));
    }
}