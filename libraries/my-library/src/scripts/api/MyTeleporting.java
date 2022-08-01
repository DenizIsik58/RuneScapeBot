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

        public static boolean feroxEnclave(){
            if (canUseTeleport(Teleport.RING_OF_DUELING_FEROX_ENCLAVE)){
                return Teleport.RING_OF_DUELING_FEROX_ENCLAVE.trigger();
            }
            return false;
        }
    }

    public static class Wealth {


        // useWealth or something, so you know thats part of it
        public static boolean grandExchange(){
            if (canUseTeleport(Teleport.RING_OF_WEALTH_GRAND_EXCHANGE)){
                return Teleport.RING_OF_WEALTH_GRAND_EXCHANGE.trigger();
            }
            return false;
        }
    }

}
