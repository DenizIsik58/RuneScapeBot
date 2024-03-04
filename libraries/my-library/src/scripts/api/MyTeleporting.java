package scripts.api;

import dax.teleports.Teleport;

public class MyTeleporting {

    public static boolean canUseTeleport(Teleport teleport) {
        if (teleport.getRequirement().satisfies()) {
            return true;
        }
        return false;
    }



    public static class Dueling {
        // MyTeleporting.Dueling.FeroxEnclave.canUseTeleport();
        // MyTeleporting.Dueling.FeroxEnclave.useTeleport();

        public static MyTeleport FeroxEnclave = () -> Teleport.RING_OF_DUELING_FEROX_ENCLAVE;
        public static MyTeleport DuelArena = () -> Teleport.RING_OF_DUELING_PVP_ARENA;
        public static MyTeleport CastleWars = () -> Teleport.RING_OF_DUELING_CASTLE_WARS;

    }

    public static class Wealth {

        public static MyTeleport GrandExchange = () -> Teleport.RING_OF_WEALTH_GRAND_EXCHANGE;
        public static MyTeleport FaladorPark = () -> Teleport.RING_OF_WEALTH_FALADOR;
        public static MyTeleport Miscellania = () -> Teleport.RING_OF_WEALTH_MISCELLANIA;

    }




    public interface MyTeleport {

        Teleport getTeleport();

        default boolean canUseTeleport() {
            return MyTeleporting.canUseTeleport(getTeleport());
        }

        default boolean useTeleport() {
            return canUseTeleport() && getTeleport().trigger();
        }

    }

}
