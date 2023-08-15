package com.robertx22.age_of_exile.maps.processors.mob;

import com.robertx22.age_of_exile.database.data.rarities.MobRarity;
import com.robertx22.age_of_exile.database.registry.ExileDB;
import com.robertx22.age_of_exile.maps.generator.ChunkProcessData;
import com.robertx22.age_of_exile.maps.mobs.SpawnedMob;
import com.robertx22.age_of_exile.maps.processors.DataProcessor;
import com.robertx22.age_of_exile.maps.processors.helpers.MobBuilder;
import com.robertx22.age_of_exile.uncommon.datasaving.Load;
import com.robertx22.age_of_exile.uncommon.utilityclasses.StringUTIL;
import com.robertx22.library_of_exile.utils.RandomUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ComplexMobProcessor extends DataProcessor {

    public ComplexMobProcessor() {
        super("spawn", Type.CONTAINS);
    }

    @Override
    public void processImplementation(String key, BlockPos pos, Level world, ChunkProcessData data) {

        try {

            String[] parts = StringUTIL.split(key, ";");

            MobRarity rarity = null;
            boolean isBoss = false;
            EntityType<? extends Mob> type = null;

            Stream<SpawnedMob> filter = null;

            int amount = 1;

            for (String x : parts) {
                int am = 0;
                try {
                    am = Integer.parseInt(x);
                } catch (NumberFormatException e) {
                }
                if (am > 0) {
                    amount = am;
                }
            }


            for (String x : parts) {
                if (ExileDB.MobRarities().isRegistered(x)) {
                    rarity = ExileDB.MobRarities().get(x);

                    if (rarity.boss) {
                        isBoss = true;
                    }
                }

            }

            if (rarity == null) {
                rarity = ExileDB.MobRarities().random();
            }

            for (String x : parts) {
                ResourceLocation loc = new ResourceLocation(x);
                if (ForgeRegistries.ENTITY_TYPES.containsKey(loc)) {
                    type = (EntityType<? extends Mob>) ForgeRegistries.ENTITY_TYPES.getValue(loc);
                }
            }

            if (type == null) {
                for (String x : parts) {
                    for (SpawnedMob.MobTags tag : SpawnedMob.MobTags.values()) {
                        if (x.equals(tag.getTagId())) {
                            filter = SpawnedMob.getAll()
                                    .stream()
                                    .filter(m -> m.tags.contains(tag));
                        }
                    }
                }
            }

            if (filter == null) {
                filter = SpawnedMob.getAll().stream().filter(x -> data.getRoom().canSpawnMob(x));
            }

            if (type == null) {
                if (isBoss) {

                    filter = filter.filter(m -> m.canBeBoss);
                }
                type = RandomUtils.weightedRandom(filter.collect(Collectors.toList())).type;
            }

            // temps
            int finalAmount = amount;
            MobRarity finalRarity = rarity;
            for (Mob mob : MobBuilder.of(type, x -> {
                x.amount = finalAmount;
                if (finalRarity != null) {
                    x.rarity = finalRarity;
                }
            }).summonMobs(world, pos)) {
                if (isBoss) {
                    Load.Unit(mob).setupRandomBoss();
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}