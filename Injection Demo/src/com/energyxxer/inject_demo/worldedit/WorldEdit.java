package com.energyxxer.inject_demo.worldedit;

import com.energyxxer.inject.InjectionMaster;
import com.energyxxer.inject.level_utils.block.BlockType;
import com.energyxxer.inject.listeners.ChatEvent;
import com.energyxxer.inject.utils.Vector3D;
import com.energyxxer.inject_demo.common.Commons;
import com.energyxxer.inject_demo.common.DisplayWindow;
import com.energyxxer.inject_demo.common.SetupListener;

import java.io.File;
import java.util.HashMap;

/**
 * Created by User on 4/13/2017.
 */
public class WorldEdit implements SetupListener{

    private static InjectionMaster master;

    private static HashMap<String, WEPlayerInfo> playerInfo = new HashMap<>();

    private WorldEdit() {
        new DisplayWindow("WorldEdit", Commons.WORLD_NAME, this);
    }

    public static void main(String[] a) {
        new WorldEdit();
    }

    @Override
    public void onSetup(String directory, String worldName) {
        master = new InjectionMaster(new File(directory + File.separator + "saves" + File.separator + worldName), new File(directory + File.separator + "logs" + File.separator + "latest.log"), "worldedit");
        master.setLogCheckFrequency(500);
        master.setInjectionFrequency(500);

        master.addLogListener(l -> {
            String text = l.getLine();
            String leadingText = "Set score of we_action for player ";
            if(!text.contains(leadingText)) return;
            text = text.substring(text.indexOf(leadingText));
            String username = text.substring(leadingText.length(),text.indexOf(' ',leadingText.length()));
            int pos = -1;
            if(text.indexOf('1',leadingText.length() + username.length()) >= 0) pos = 1;
            else if(text.indexOf('2',leadingText.length() + username.length()) >= 0) pos = 2;

            if(pos == -1) return;
            if(playerInfo.containsKey(username)) {
                playerInfo.get(username).updateEditPos(pos, master);
            } else {
                playerInfo.put(username, new WEPlayerInfo(username));
                master.injector.insertImpulseCommand("tellraw @a {\"text\":\"[WARNING]: Player '" + username + "' requested an action but wasn't found on the player database.\",\"color\":\"yellow\"}");
            }
        });

        master.addChatListener(m -> {
            String[] args = m.getMessage().split(" ");
            if(args.length > 0) {
                if(args[0].equals("..pos1") || args[0].equals("..pos2")) {
                    int index = (args[0].endsWith("1")) ? 1 : 2;
                    if(args.length > 1) {
                        if(args.length != 4) {
                            master.injector.insertImpulseCommand("tellraw " + m.getSender() + "{\"text\":\"Usage: ..pos" + index + " <x y z>\",\"color\":\"red\"}");
                        } else {
                            try {
                                int x = Integer.parseInt(args[1]);
                                int y = Integer.parseInt(args[2]);
                                int z = Integer.parseInt(args[3]);
                                WEPlayerInfo player = playerInfo.get(m.getSender());
                                player.updateEditPos(index, new Vector3D(x,y,z), master);
                            } catch(NumberFormatException x) {
                                master.injector.insertImpulseCommand("tellraw " + m.getSender() + "{\"text\":\"Usage: ..pos" + index + " <x y z>\",\"color\":\"red\"}");
                            }
                        }
                    } else {
                        master.injector.insertFetchCommand("execute " + m.getSender() + " ~ ~ ~ summon area_effect_cloud ~ ~ ~ {CustomName:\"$weTransform\",Duration:2}");
                        master.injector.insertFetchCommand("tp @e[type=area_effect_cloud,name=$weTransform,c=1] " + m.getSender());
                        master.injector.insertFetchCommand("execute " + m.getSender() + " ~ ~ ~ entitydata @e[type=area_effect_cloud,name=$weTransform,c=1] {we:transform}");
                        master.injector.insertFetchCommand("testfor " + m.getSender(), e -> {
                            WEPlayerInfo player = playerInfo.get(m.getSender());
                            player.updateEditPos(index, player.transform.asVector().asIntVector(), master);
                        });
                    }
                } else if(args[0].equals("..set")) {
                    if(args.length == 2) {
                        WEPlayerInfo player = playerInfo.get(m.getSender());
                        if(player != null && player.pos1 != null && player.pos2 != null) {
                            String block = parseBlockReference(args[1], "default", m);
                            if(block == null) return;
                            master.injector.insertImpulseCommand("title " + m.getSender() + " actionbar {\"text\":\"Setting region to " + BlockType.valueOf(block.split(" ")[0].toUpperCase()).name + "\",\"color\":\"dark_aqua\"}");
                            splitFill(master, player.pos1, player.pos2, block);
                        } else {
                            master.injector.insertImpulseCommand("tellraw " + m.getSender() + " {\"text\":\"You must first select two points!\",\"color\":\"red\"}");
                        }
                    } else {
                        master.injector.insertImpulseCommand("tellraw " + m.getSender() + " {\"text\":\"Usage: ..set <block>\",\"color\":\"red\"}");
                    }
                } else if(args[0].equals("..walls")) {
                    if(args.length >= 2 && args.length <= 3) {
                        WEPlayerInfo player = playerInfo.get(m.getSender());
                        if(player != null && player.pos1 != null && player.pos2 != null) {
                            String block = parseBlockReference(args[1], (args.length > 2) ? "*" : "default", m);
                            if(block == null) return;
                            String block2 = null;
                            if(args.length > 2) {
                                block2 = parseBlockReference(args[2], "default", m);
                                if(block2 == null) return;
                            }
                            String postCoordinateArgs = (args.length > 2) ? block2 + " replace " + block : block;
                            master.injector.insertImpulseCommand("title " + m.getSender() + " actionbar {\"text\":\"Setting region's walls to " + BlockType.valueOf(block.split(" ")[0].toUpperCase()).name + "\",\"color\":\"dark_aqua\"}");
                            splitFill(master, new Vector3D(player.pos1.x, player.pos1.y, player.pos1.z), new Vector3D(player.pos2.x, player.pos2.y, player.pos1.z), postCoordinateArgs);
                            splitFill(master, new Vector3D(player.pos1.x, player.pos1.y, player.pos1.z), new Vector3D(player.pos1.x, player.pos2.y, player.pos2.z), postCoordinateArgs);
                            splitFill(master, new Vector3D(player.pos2.x, player.pos1.y, player.pos2.z), new Vector3D(player.pos1.x, player.pos2.y, player.pos2.z), postCoordinateArgs);
                            splitFill(master, new Vector3D(player.pos2.x, player.pos1.y, player.pos2.z), new Vector3D(player.pos2.x, player.pos2.y, player.pos1.z), postCoordinateArgs);
                        } else {
                            master.injector.insertImpulseCommand("tellraw " + m.getSender() + " {\"text\":\"You must first select two points!\",\"color\":\"red\"}");
                        }
                    } else {
                        master.injector.insertImpulseCommand("tellraw " + m.getSender() + " {\"text\":\"Usage: ..walls <block>\",\"color\":\"red\"}");
                    }
                } else if(args[0].equals("..replace")) {
                    if(args.length == 3) {
                        WEPlayerInfo player = playerInfo.get(m.getSender());
                        if(player != null && player.pos1 != null && player.pos2 != null) {
                            String target = parseBlockReference(args[1], "*", m);
                            if(target == null) return;
                            String replacement = parseBlockReference(args[2], "default", m);
                            if(replacement == null) return;
                            master.injector.insertImpulseCommand("title " + m.getSender() + " actionbar {\"text\":\"Replacing " + BlockType.valueOf(target.split(" ")[0].toUpperCase()).name + " with " + BlockType.valueOf(replacement.split(" ")[0].toUpperCase()).name + "\",\"color\":\"dark_aqua\"}");
                            splitFill(master, player.pos1, player.pos2, replacement + " replace " + target);
                        } else {
                            master.injector.insertImpulseCommand("tellraw " + m.getSender() + " {\"text\":\"You must first select two points!\",\"color\":\"red\"}");
                        }
                    } else {
                        master.injector.insertImpulseCommand("tellraw " + m.getSender() + " {\"text\":\"Usage:\\n    ..replace <target> <replacement>\",\"color\":\"red\"}");
                    }
                } else if(args[0].equals("..up")) {
                    if(args.length == 2) {
                        try {
                            int amount = Integer.parseInt(args[1]);
                            master.injector.insertImpulseCommand("execute " + m.getSender() + " ~ ~ ~ summon leash_knot ~ ~" + amount + " ~ {CustomName:\"we_up_" + m.getSender() + "\"}");
                            master.injector.insertImpulseCommand("execute @e[type=leash_knot,name=we_up_" + m.getSender() + "] ~ ~ ~ teleport " + m.getSender() + " ~ ~-0.5 ~");
                            master.injector.insertImpulseCommand("execute @e[type=leash_knot,name=we_up_" + m.getSender() + "] ~ ~ ~ setblock ~ ~-1 ~ glass");
                            master.injector.insertImpulseCommand("kill @e[type=leash_knot,name=we_up_" + m.getSender() + "]");
                            master.injector.insertImpulseCommand("title " + m.getSender() + " actionbar {\"text\":\"Whoosh!\",\"color\":\"yellow\"}");
                        } catch(NumberFormatException x) {
                            master.injector.insertImpulseCommand("tellraw " + m.getSender() + " {\"text\":\"Usage:\\n    ..up <number of blocks>\",\"color\":\"red\"}");
                        }
                    } else {
                        master.injector.insertImpulseCommand("tellraw " + m.getSender() + " {\"text\":\"Usage:\\n    ..up <number of blocks>\",\"color\":\"red\"}");
                    }
                } else if(args[0].equals("..center")) {
                    if(args.length == 2) {
                        WEPlayerInfo player = playerInfo.get(m.getSender());
                        if(player != null && player.pos1 != null && player.pos2 != null) {
                            String block = parseBlockReference(args[1], "default", m);
                            if(block == null) return;
                            master.injector.insertImpulseCommand("title " + m.getSender() + " actionbar {\"text\":\"Setting region's center to " + BlockType.valueOf(block.split(" ")[0].toUpperCase()).name + "\",\"color\":\"dark_aqua\"}");
                            splitFill(master,
                                    new Vector3D(
                                            (int) Math.floor((player.pos2.x+player.pos1.x)/2.0),
                                            (int) Math.floor((player.pos2.y+player.pos1.y)/2.0),
                                            (int) Math.floor((player.pos2.z+player.pos1.z)/2.0)),
                                    new Vector3D(
                                            (int) Math.ceil((player.pos2.x+player.pos1.x)/2.0),
                                            (int) Math.ceil((player.pos2.y+player.pos1.y)/2.0),
                                            (int) Math.ceil((player.pos2.z+player.pos1.z)/2.0)),
                                    block);
                        } else {
                            master.injector.insertImpulseCommand("tellraw " + m.getSender() + " {\"text\":\"You must first select two points!\",\"color\":\"red\"}");
                        }
                    } else {
                        master.injector.insertImpulseCommand("tellraw " + m.getSender() + " {\"text\":\"Usage: ..center <block>\",\"color\":\"red\"}");
                    }
                }
            }
        });

        master.addLogListener(e -> {
            String line = e.getLine();
            if(line.startsWith("[Server thread/INFO]: [",11) && line.contains(",we:\"transform\",")) {
                if(line.indexOf(':',34) < 0) return;
                String username = line.substring(34,line.indexOf(':',34));

                WEPlayerInfo player;

                if(playerInfo.containsKey(username)) {
                    player = playerInfo.get(username);
                } else {
                    player = new WEPlayerInfo(username);
                    playerInfo.put(username, player);
                }

                {
                    int posIndex = line.indexOf(",Pos:[", 34 + username.length()) + ",Pos:[".length();
                    if (posIndex < ",Pos:[".length()) return;
                    String rawPos = line.substring(posIndex, line.indexOf(']', posIndex)).replaceAll("\\d:","").replace("d", "").replace(",", " ");
                    String[] pos = rawPos.split(" ");
                    player.transform.x = Double.parseDouble(pos[0]);
                    player.transform.y = Double.parseDouble(pos[1]);
                    player.transform.z = Double.parseDouble(pos[2]);
                }

                {
                    int rotIndex = line.indexOf(",Rotation:[", 34 + username.length()) + ",Rotation:[".length();
                    if (rotIndex < ",Rotation:[".length()) return;
                    String rawRot = line.substring(rotIndex, line.indexOf(']', rotIndex)).replaceAll("\\d:","").replace("f", "").replace(",", " ");
                    String[] rot = rawRot.split(" ");
                    player.transform.yaw = Double.parseDouble(rot[0]);
                    player.transform.pitch = Double.parseDouble(rot[1]);
                }
            }
        });

        master.start();
    }

    private static String parseBlockReference(String str, String defData, ChatEvent e) {
        if(str.contains(":") && str.contains("#")) {
            master.injector.insertImpulseCommand("tellraw " + e.getSender() + " {\"text\":\"Cannot have both data values and blockstates in a block reference: " + str + "\",\"color\":\"red\"}");
            return null;
        }
        boolean blockstate = str.contains("#");
        String rawBlock[] = str.split("[:#]");
        if((str.contains(":") || str.contains("#")) && rawBlock.length == 1) {
            master.injector.insertImpulseCommand("tellraw " + e.getSender() + " {\"text\":\"Invalid block reference: " + str + "\",\"color\":\"red\"}");
            return null;
        }
        String blockType = rawBlock[0];

        String data = defData;

        try {
            BlockType.valueOf(blockType.toUpperCase());
        } catch(IllegalArgumentException x) {
            master.injector.insertImpulseCommand("tellraw " + e.getSender() + " {\"text\":\"No block with id '" + blockType + "' exists\",\"color\":\"red\"}");
            return null;
        }

        if(!blockstate) {
            int dataValue = -1;
            if(rawBlock.length > 1) {
                try {
                    dataValue = Integer.parseInt(rawBlock[1]);
                } catch(NumberFormatException x) {
                    master.injector.insertImpulseCommand("tellraw " + e.getSender() + " {\"text\":\"Invalid data value: " + dataValue + "\",\"color\":\"red\"}");
                    return null;
                }
                if(dataValue < 0 || dataValue > 15) {
                    master.injector.insertImpulseCommand("tellraw " + e.getSender() + " {\"text\":\"Invalid data value: " + dataValue + ". Must be between 0 and 15\",\"color\":\"red\"}");
                    return null;
                }
            }
            if(dataValue > -1) data = Integer.toString(dataValue);
        } else {
            data = rawBlock[1];
        }

        return (blockType + " " + data).trim();
    }

    private static void splitFill(InjectionMaster master, Vector3D rawPos1, Vector3D rawPos2, String postCoordinateArgs) {
        Vector3D pos1 = new Vector3D(Math.min(rawPos1.x, rawPos2.x), Math.min(rawPos1.y, rawPos2.y), Math.min(rawPos1.z, rawPos2.z));
        Vector3D pos2 = new Vector3D(Math.max(rawPos1.x, rawPos2.x), Math.max(rawPos1.y, rawPos2.y), Math.max(rawPos1.z, rawPos2.z));
        Vector3D size = new Vector3D(pos2.x-pos1.x+1, pos2.y-pos1.y+1, pos2.z-pos1.z+1);
        recSplitFill(master, pos1, size, postCoordinateArgs);
    }

    private static void recSplitFill(InjectionMaster master, Vector3D pos, Vector3D size, String postCoordinateArgs) {
        if(size.x > 32) {
            recSplitFill(master, pos.translated(32,0,0), size.translated(-32,0,0), postCoordinateArgs);
            size.x = 32;
        }
        if(size.y > 32) {
            recSplitFill(master, pos.translated(0,32,0), size.translated(0,-32,0), postCoordinateArgs);
            size.y = 32;
        }
        if(size.z > 32) {
            recSplitFill(master, pos.translated(0,0,32), size.translated(0,0,-32), postCoordinateArgs);
            size.z = 32;
        }
        if(size.x == 0 || size.y == 0 || size.z == 0) return;
        master.injector.insertImpulseCommand("fill " + pos + " " + pos.translated(size.x-1,size.y-1,size.z-1) + " " + postCoordinateArgs);
    }

}