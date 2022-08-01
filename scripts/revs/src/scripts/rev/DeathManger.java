package scripts.rev;


public class DeathManger {

    private static int totalDeaths = 0;

    public static void reGearFromDeath(){
        BankManagerRevenant.init();
    }

    public static void incrementTotalDeaths(){
        totalDeaths++;
    }

    public static int totalDeaths() {
        return totalDeaths;
    }
}
