package com.energyxxer.inject_demo.worldedit;

import static com.energyxxer.inject.InjectionBuffer.InjectionType.IMPULSE;
import static de.adrodoc55.minecraft.coordinate.Axis3.Y;

import java.util.Arrays;
import java.util.List;

import com.energyxxer.inject.InjectionConnection;
import com.energyxxer.inject.level_utils.LevelReader;
import com.energyxxer.inject.level_utils.block.BlockType;
import com.energyxxer.inject_demo.util.Transform;

import de.adrodoc55.minecraft.coordinate.Vec3D;
import de.adrodoc55.minecraft.coordinate.Vec3I;

/**
 * Created by User on 4/13/2017.
 */
public class WEPlayerInfo {
    private static int NEXT_ID = 0;
    private static double EYE_LEVEL = 1.68125;

    final String username;
    final int id;
    Transform transform = new Transform();
    Vec3I pos1 = null;
    Vec3I pos2 = null;

    public WEPlayerInfo(String username) {
        this.username = username;
        this.id = NEXT_ID++;
    }

    public void updateEditPos(int index, Vec3I pos, InjectionConnection connection) {
        connection.inject(IMPULSE, "summon shulker " + pos.toAbsoluteString() + " {CustomName:\"wePosMarker\",NoGravity:1b,NoAI:1b,Glowing:1b,Invulnerable:1b,Silent:1b,ActiveEffects:[{Id:14b,Duration:1000s,Amplifier:0b,ShowParticles:0b}]}");
        connection.inject(IMPULSE, "tellraw " + username + " [{\"text\":\"[\",\"color\":\"dark_aqua\"},{\"text\":\"WorldEdit\",\"color\":\"aqua\"},\"] Position " + index + " set to (" + pos.toAbsoluteString() + ")\"]");
        if(index == 1) pos1 = pos;
        else if(index == 2) pos2 = pos;
    }

    public void updateEditPos(int index, InjectionConnection connection, LevelReader reader) {
        for(double d = 0; d <= 5; d += 0.25) {
            Vec3D forward = transform.forward(d);
            Vec3I pos = forward.plus(EYE_LEVEL, Y).floor();
            BlockType blockType = reader.getBlockAtPos(pos).type;
            List<BlockType> ignore = Arrays.asList(BlockType.AIR, BlockType.WATER, BlockType.FLOWING_WATER, BlockType.LAVA, BlockType.FLOWING_LAVA);
            if(!ignore.contains(blockType))  {
                connection.inject(IMPULSE, "summon shulker " + pos.toAbsoluteString() + " {CustomName:\"wePosMarker\",NoGravity:1b,NoAI:1b,Glowing:1b,Invulnerable:1b,Silent:1b,ActiveEffects:[{Id:14b,Duration:1000s,Amplifier:0b,ShowParticles:0b}]}");
                connection.inject(IMPULSE, "tellraw " + username + " [{\"text\":\"[\",\"color\":\"dark_aqua\"},{\"text\":\"WorldEdit\",\"color\":\"aqua\"},\"] Position " + index + " set to (" + pos.toAbsoluteString() + ")\"]");
                if(index == 1) pos1 = pos;
                else if(index == 2) pos2 = pos;
                return;
            }
        }
    }
}
