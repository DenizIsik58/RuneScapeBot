package scripts;

public enum Axe {

    BRONZE(1351),
    IRON(1349),
    MITHRIL(1355),
    ADAMANT(1357),
    RUNE(1359);

    private final int axeId;

     Axe(int axeId){
        this.axeId = axeId;
    }

    public int getAxeId() {
        return axeId;
    }
}
