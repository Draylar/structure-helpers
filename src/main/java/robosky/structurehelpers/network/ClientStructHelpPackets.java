package robosky.structurehelpers.network;

import robosky.structurehelpers.StructureHelpers;
import robosky.structurehelpers.block.LootDataBlockEntity;
import robosky.structurehelpers.client.LootDataScreen;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

public final class ClientStructHelpPackets {

    private ClientStructHelpPackets() {
    }

    public static final Identifier LOOT_DATA_OPEN =
        new Identifier(StructureHelpers.MODID, "loot_data_open");

    private static void openLootDataScreen(MinecraftClient client, ClientPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        LootDataPacketData data = new LootDataPacketData();
        data.read(buf);
        client.execute(() -> {
            BlockEntity be = client.world.getBlockEntity(data.getPos());
            if(be instanceof LootDataBlockEntity) {
                LootDataBlockEntity ld = (LootDataBlockEntity)be;
                ld.setLootTable(data.getLootTable());
                ld.setReplacementState(data.getReplacement());
                client.openScreen(new LootDataScreen(ld));
            }
        });
    }

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(LOOT_DATA_OPEN, ClientStructHelpPackets::openLootDataScreen);
    }
}
